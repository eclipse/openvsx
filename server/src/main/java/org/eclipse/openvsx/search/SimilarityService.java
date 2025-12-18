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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.annotation.Nullable;

@Service
public class SimilarityService {
    private static final Logger logger = LoggerFactory.getLogger(SimilarityService.class);

    private final SimilarityConfig config;
    private final RepositoryService repositories;

    public SimilarityService(
            SimilarityConfig config,
            RepositoryService repositories
    ) {
        this.config = config;
        this.repositories = repositories;
    }

    /**
     * Check if any of the given fields (name, namespace, displayName) are too similar 
     * to any existing extensions (verified only or all, based on configuration).
     */
    public List<Extension> findSimilarExtensions(
            @Nullable String extensionName, 
            @Nullable String namespaceName, 
            @Nullable String displayName,
            @NotNull List<String> excludeNamespaces) {

        if (!config.isEnabled()) {
            return List.of();
        }

        if (extensionName == null && namespaceName == null && displayName == null) {
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

        boolean verifiedOnly = config.isCheckAgainstVerifiedOnly();
        
        try {
            return repositories.findSimilarExtensionsByLevenshtein(
                extensionName,
                namespaceName,
                displayName,
                excludeNamespaces,
                config.getLevenshteinThreshold(),
                verifiedOnly,
                10 // Limit to top 10 most similar extensions
            );
        } catch (Exception e) {
            logger.error("Similarity check failed for extension='{}', namespace='{}', displayName='{}': {}", 
                extensionName, namespaceName, displayName, e.getMessage(), e);
            
            throw new RuntimeException(
                "Unable to verify extension name uniqueness due to system error. " +
                "Please try again later or contact support if the problem persists."
            );
        }
    }

    public List<Extension> findSimilarExtensions(@NotNull ExtensionVersion extVersion) {
        String extensionName = null;
        String namespaceName = null;

        if (extVersion.getExtension() != null) {
            extensionName = extVersion.getExtension().getName();
            if (extVersion.getExtension().getNamespace() != null) {
                namespaceName = extVersion.getExtension().getNamespace().getName();
            }
        }

        String displayName = extVersion.getDisplayName();

        // Exclude the extension's own namespace
        List<String> excludeNamespaces = namespaceName != null ? List.of(namespaceName) : List.of();
        return findSimilarExtensions(extensionName, namespaceName, displayName, excludeNamespaces);
    }

    /**
     * Check if the proposed namespace name is too similar to any existing namespace names.
     * This helps prevent namespace squatting and confusing similar names.
     */
    public List<Namespace> findSimilarNamespaces(@NotNull String namespaceName, @NotNull List<String> excludeNamespaces) {
        // If similarity checking is disabled, allow all names
        if (!config.isEnabled()) {
            return List.of();
        }

        // If no name provided, nothing to check
        if (namespaceName.isEmpty()) {
            return List.of();
        }

        boolean verifiedOnly = config.isCheckAgainstVerifiedOnly();
        
        try {
            return repositories.findSimilarNamespacesByLevenshtein(
                namespaceName,
                excludeNamespaces,
                config.getLevenshteinThreshold(),
                verifiedOnly,
                10
            );
        } catch (Exception e) {
            logger.error("Similarity check failed for namespace='{}': {}", 
                namespaceName, e.getMessage(), e);
            
            throw new RuntimeException(
                "Unable to verify namespace name uniqueness due to system error. " +
                "Please try again later or contact support if the problem persists."
            );
        }
    }

}