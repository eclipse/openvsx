/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.openvsx.admin;

import org.eclipse.openvsx.entities.Tier;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.TierJson;
import org.eclipse.openvsx.json.TierListJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/ratelimit")
public class RateLimitAPI {
    private final Logger logger = LoggerFactory.getLogger(RateLimitAPI.class);

    private final RepositoryService repositories;
    private final AdminService admins;

    public RateLimitAPI(
            RepositoryService repositories,
            AdminService admins
    ) {
        this.repositories = repositories;
        this.admins = admins;
    }

    @GetMapping(
            path = "/tiers",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TierListJson> getTiers() {
        try {
            admins.checkAdminUser();

            var tiers = repositories.findAllTiers();
            var result = new TierListJson(tiers.stream().map(Tier::toJson).toList());
            return ResponseEntity.ok(result);
        } catch (Exception exc) {
            logger.error("failed retrieving tiers", exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
            path = "/tiers/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TierJson> createTier(@RequestBody TierJson tier) {
        try {
            admins.checkAdminUser();

            var existingTier = repositories.findTier(tier.name());
            if (existingTier != null) {
                return ResponseEntity.badRequest().build();
            }

            var savedTier = repositories.upsertTier(Tier.fromJson(tier));
            return ResponseEntity.ok(savedTier.toJson());
        } catch (Exception exc) {
            logger.error("failed creating tier {}", tier.name(), exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(
            path = "/tiers/{name}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TierJson> updateTier(@PathVariable String name, @RequestBody TierJson tier) {
        try {
            admins.checkAdminUser();

            var savedTier = repositories.findTier(name);
            if (savedTier == null) {
                return ResponseEntity.notFound().build();
            }

            savedTier.updateFromJson(tier);
            savedTier = repositories.upsertTier(savedTier);

            return ResponseEntity.ok(savedTier.toJson());
        } catch (Exception exc) {
            logger.error("failed updating tier {}", name, exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping(
            path = "/tiers/{name}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteTier(@PathVariable String name) {
        try {
            admins.checkAdminUser();

            var tier = repositories.findTier(name);
            if (tier == null) {
                return ResponseEntity.notFound().build();
            }

            var existingCustomers = repositories.countCustomersByTier(tier);
            if (existingCustomers > 0) {
                return ResponseEntity.badRequest().body(ResultJson.error("Cannot delete tier '" + name + "' because it is still in use"));
            }

            repositories.deleteTier(tier);

            return ResponseEntity.ok(ResultJson.success("Deleted tier '" + name + "'"));
        } catch (Exception exc) {
            logger.error("failed deleting tier {}", name, exc);
            return ResponseEntity.internalServerError().build();
        }
    }
}
