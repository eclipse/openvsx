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

import org.eclipse.openvsx.entities.Customer;
import org.eclipse.openvsx.entities.Tier;
import org.eclipse.openvsx.json.*;
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
            var adminUser = admins.checkAdminUser();

            var existingTier = repositories.findTier(tier.name());
            if (existingTier != null) {
                return ResponseEntity.badRequest().build();
            }

            var savedTier = repositories.upsertTier(Tier.fromJson(tier));

            var result = ResultJson.success("Created tier '" + savedTier.getName() + "'");
            admins.logAdminAction(adminUser, result);

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
            var adminUser = admins.checkAdminUser();

            var savedTier = repositories.findTier(name);
            if (savedTier == null) {
                return ResponseEntity.notFound().build();
            }

            savedTier.updateFromJson(tier);
            savedTier = repositories.upsertTier(savedTier);

            var result = ResultJson.success("Updated tier '" + savedTier.getName() + "'");
            admins.logAdminAction(adminUser, result);

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
            var adminUser = admins.checkAdminUser();

            var tier = repositories.findTier(name);
            if (tier == null) {
                return ResponseEntity.notFound().build();
            }

            var existingCustomers = repositories.countCustomersByTier(tier);
            if (existingCustomers > 0) {
                return ResponseEntity.badRequest().body(ResultJson.error("Cannot delete tier '" + name + "' because it is still in use"));
            }

            repositories.deleteTier(tier);

            var result = ResultJson.success("Deleted tier '" + name + "'");
            admins.logAdminAction(adminUser, result);

            return ResponseEntity.ok(result);
        } catch (Exception exc) {
            logger.error("failed deleting tier {}", name, exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(
            path = "/customers",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CustomerListJson> getCustomers() {
        try {
            admins.checkAdminUser();

            var customers = repositories.findAllCustomers();
            var result = new CustomerListJson(customers.stream().map(Customer::toJson).toList());
            return ResponseEntity.ok(result);
        } catch (Exception exc) {
            logger.error("failed retrieving customers", exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
            path = "/customers/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CustomerJson> createCustomer(@RequestBody CustomerJson customerJson) {
        try {
            var adminUser = admins.checkAdminUser();

            var existingCustomer = repositories.findCustomer(customerJson.name());
            if (existingCustomer != null) {
                return ResponseEntity.badRequest().build();
            }

            var customer = Customer.fromJson(customerJson);
            // resolve the tier reference
            var tier = repositories.findTier(customer.getTier().getName());
            if (tier == null) {
                return ResponseEntity.badRequest().build();
            }
            customer.setTier(tier);

            var savedCustomer = repositories.upsertCustomer(customer);

            var result = ResultJson.success("Created customer '" + savedCustomer.getName() + "'");
            admins.logAdminAction(adminUser, result);

            return ResponseEntity.ok(savedCustomer.toJson());
        } catch (Exception exc) {
            logger.error("failed creating customer {}", customerJson.name(), exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(
            path = "/customers/{name}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CustomerJson> updateCustomer(@PathVariable String name, @RequestBody CustomerJson customer) {
        try {
            var adminUser = admins.checkAdminUser();

            var savedCustomer = repositories.findCustomer(name);
            if (savedCustomer == null) {
                return ResponseEntity.notFound().build();
            }

            savedCustomer.updateFromJson(customer);
            // update the tier reference in case it changed
            var tier = repositories.findTier(savedCustomer.getTier().getName());
            if (tier == null) {
                return ResponseEntity.badRequest().build();
            }
            savedCustomer.setTier(tier);

            savedCustomer = repositories.upsertCustomer(savedCustomer);

            var result = ResultJson.success("Updated customer '" + savedCustomer.getName() + "'");
            admins.logAdminAction(adminUser, result);

            return ResponseEntity.ok(savedCustomer.toJson());
        } catch (Exception exc) {
            logger.error("failed updating tier {}", name, exc);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping(
            path = "/customers/{name}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteCustomer(@PathVariable String name) {
        try {
            var adminUser = admins.checkAdminUser();

            var customer = repositories.findCustomer(name);
            if (customer == null) {
                return ResponseEntity.notFound().build();
            }

            repositories.deleteCustomer(customer);

            var result = ResultJson.success("Deleted customer '" + name + "'");
            admins.logAdminAction(adminUser, result);

            return ResponseEntity.ok(result);
        } catch (Exception exc) {
            logger.error("failed deleting customer {}", name, exc);
            return ResponseEntity.internalServerError().build();
        }
    }
}
