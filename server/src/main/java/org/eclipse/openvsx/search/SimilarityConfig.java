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

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for extension similarity checking.
 * 
 * Configuration example:
 * ovsx:
 *   similarity:
 *     enabled: true
 *     new-extensions-only: false  # If true, only check the very first upload of an extension (no prior versions)
 *     levenshtein-threshold: 0.15  # Block if distance <= 0.15 * length (allows max 15% difference)
 *     skip-verified-publishers: true  # Don't check verified publishers' new extensions
 *     check-against-verified-only: true  # Only compare against verified publishers' extensions
 *     exclude-owner-namespaces: true  # Exclude namespaces where user is an owner from similarity checks
 */
@Configuration
public class SimilarityConfig {
    @Value("${ovsx.similarity.enabled:false}")
    private boolean enabled;

    @Value("${ovsx.similarity.new-extensions-only:false}")
    private boolean newExtensionsOnly;

    @Value("${ovsx.similarity.levenshtein-threshold:0.15}")
    private double levenshteinThreshold;

    @Value("${ovsx.similarity.skip-verified-publishers:true}")
    private boolean skipVerifiedPublishers;

    @Value("${ovsx.similarity.check-against-verified-only:true}")
    private boolean checkAgainstVerifiedOnly;

    @Value("${ovsx.similarity.exclude-owner-namespaces:true}")
    private boolean excludeOwnerNamespaces;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNewExtensionsOnly() {
        return newExtensionsOnly;
    }

    public double getLevenshteinThreshold() {
        return levenshteinThreshold;
    }

    public boolean isSkipVerifiedPublishers() {
        return skipVerifiedPublishers;
    }

    public boolean isCheckAgainstVerifiedOnly() {
        return checkAgainstVerifiedOnly;
    }

    public boolean isExcludeOwnerNamespaces() {
        return excludeOwnerNamespaces;
    }

    @PostConstruct
    public void validate() {
        if (levenshteinThreshold < 0.0 || levenshteinThreshold > 0.3) {
            throw new IllegalArgumentException(
                "ovsx.similarity.levenshtein-threshold must be between 0.0 and 0.3, got: " + levenshteinThreshold);
        }
    }
}