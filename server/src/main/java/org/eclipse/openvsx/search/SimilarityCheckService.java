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
import org.eclipse.openvsx.scanning.ValidationCheck;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Central entry point for enforcing similarity rules with configuration-based policy.
 *
 * This service reads {@link SimilarityConfig} and applies all policy decisions.
 * Implements ValidationCheck to be auto-discovered by ExtensionScanService.
 */
@Service
@ConditionalOnProperty(name = "ovsx.similarity.enabled", havingValue = "true")
public class SimilarityCheckService implements ValidationCheck {

    public static final String CHECK_TYPE = "NAME_SQUATTING";

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

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public boolean isEnforced() {
        return config.isEnforced();
    }

    @Override
    public String getCheckType() {
        return CHECK_TYPE;
    }

    @Override
    public ValidationCheck.Result check(ValidationCheck.Context context) {
        var scan = context.scan();
        var namespaceName = scan.getNamespaceName();
        var extensionName = scan.getExtensionName();
        var displayName = scan.getExtensionDisplayName();

        if (config.isNewExtensionsOnly() && repositories.countVersions(namespaceName, extensionName) > 1) {
            return ValidationCheck.Result.pass();
        }

        if (config.isSkipVerifiedPublishers()) {
            var namespace = repositories.findNamespace(namespaceName);
            if (namespace != null && repositories.hasMemberships(namespace, NamespaceMembership.ROLE_OWNER)) {
                return ValidationCheck.Result.pass();
            }
        }

        var similarExtensions = findSimilarExtensionsForPublishing(
            extensionName,
            namespaceName,
            displayName,
            context.user()
        );

        if (similarExtensions.isEmpty()) {
            return ValidationCheck.Result.pass();
        }

        var similarExt = similarExtensions.get(0);
        var latestVersion = repositories.findLatestVersion(similarExt, null, false, true);
        String similarDisplayName = latestVersion != null ? latestVersion.getDisplayName() : null;

        var reason = String.format(
            "Extension '%s.%s' (display name: '%s') is too similar to existing extension '%s.%s' (display name: '%s')",
            namespaceName,
            extensionName,
            displayName,
            similarExt.getNamespace().getName(),
            similarExt.getName(),
            similarDisplayName != null ? similarDisplayName : ""
        );

        return ValidationCheck.Result.fail("Levenshtein Distance", reason);
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


