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

import jakarta.validation.constraints.NotNull;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
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
     * Returns whether similarity checking is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Enforce configured similarity rules for publishing an extension.
     * Callers should check {@link #isEnabled()} before invoking this method.
     */
    public List<Extension> findSimilarExtensionsForPublishing(
            @Nullable String extensionName,
            @Nullable String namespaceName,
            @Nullable String displayName,
            @NotNull UserData publishingUser
    ) {
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

        return similarityService.findSimilarExtensions(
            extensionName,
            namespaceName,
            displayName,
            getExcludedNamespaces(publishingUser),
            config.getLevenshteinThreshold(),
            config.isCheckAgainstVerifiedOnly(),
            LIMIT
        );
    }

    /**
     * Enforce configured similarity rules for namespace creation.
     * Callers should check {@link #isEnabled()} before invoking this method.
     */
    public List<Namespace> findSimilarNamespacesForCreation(
            @NotNull String namespaceName,
            @NotNull UserData publishingUser
    ) {
        return similarityService.findSimilarNamespaces(
            namespaceName,
            getExcludedNamespaces(publishingUser),
            config.getLevenshteinThreshold(),
            config.isCheckAgainstVerifiedOnly(),
            LIMIT
        );
    }

    /**
     * Get the list of namespaces to exclude from similarity checks.
     * When configured, excludes namespaces where the user is an owner.
     */
    private List<String> getExcludedNamespaces(@NotNull UserData user) {
        if (!config.isExcludeOwnerNamespaces()) {
            return List.of();
        }
        return repositories.findMemberships(user)
                .stream()
                .filter(m -> NamespaceMembership.ROLE_OWNER.equals(m.getRole()))
                .map(m -> m.getNamespace().getName())
                .toList();
    }
}


