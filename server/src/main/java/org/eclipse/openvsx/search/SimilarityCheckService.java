/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central entry point for enforcing similarity rules with configuration-based policy.
 *
 * This service reads {@link SimilarityConfig} and applies all policy decisions.
 */
@Service
public class SimilarityCheckService {

    private static final int LIMIT = 10;

    private final SimilarityConfig config;
    private final SimilarityService similarityService;
    private final RepositoryService repositories;

    public SimilarityCheckService(
            SimilarityConfig config,
            SimilarityService similarityService,
            RepositoryService repositories
    ) {
        this.config = config;
        this.similarityService = similarityService;
        this.repositories = repositories;
    }

    /**
     * Enforce configured similarity rules for publishing an extension.
     */
    public List<Extension> findSimilarExtensionsForPublishing(
            String extensionName,
            String namespaceName,
            String displayName,
            UserData publishingUser
    ) {
        if (!config.isEnabled()) {
            return List.of();
        }

        if (config.isNewExtensionsOnly() && namespaceName != null && extensionName != null) {
            if (repositories.countVersions(namespaceName, extensionName) > 0) {
                return List.of();
            }
        }

        if (config.isSkipVerifiedPublishers() && namespaceName != null) {
            var namespace = repositories.findNamespace(namespaceName);
            if (namespace != null && repositories.hasMemberships(namespace, NamespaceMembership.ROLE_OWNER)) {
                return List.of();
            }
        }

        List<String> excludeNamespaces = config.isExcludeOwnerNamespaces()
                ? repositories.findMemberships(publishingUser)
                    .stream()
                    .filter(membership -> NamespaceMembership.ROLE_OWNER.equals(membership.getRole()))
                    .map(membership -> membership.getNamespace().getName())
                    .toList()
                : List.of();

        return similarityService.findSimilarExtensions(
            extensionName,
            namespaceName,
            displayName,
            excludeNamespaces,
            config.getLevenshteinThreshold(),
            config.isCheckAgainstVerifiedOnly(),
            LIMIT
        );
    }

    /**
     * Enforce configured similarity rules for namespace creation.
     */
    public List<Namespace> findSimilarNamespacesForCreation(String namespaceName, UserData publishingUser) {
        if (!config.isEnabled()) {
            return List.of();
        }

        List<String> excludeNamespaces = config.isExcludeOwnerNamespaces()
                ? repositories.findMemberships(publishingUser)
                    .stream()
                    .filter(membership -> NamespaceMembership.ROLE_OWNER.equals(membership.getRole()))
                    .map(membership -> membership.getNamespace().getName())
                    .toList()
                : List.of();

        return similarityService.findSimilarNamespaces(
            namespaceName,
            excludeNamespaces,
            config.getLevenshteinThreshold(),
            config.isCheckAgainstVerifiedOnly(),
            LIMIT
        );
    }
}


