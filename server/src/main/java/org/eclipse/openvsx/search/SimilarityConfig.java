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
 *     enabled: false  # Enables/disables similarity checks during publishing.
 *     new-extensions-only: false  # If true, only check the very first upload (no prior versions).
 *     levenshtein-threshold: 0.15  # Valid range: 0.0 - 0.3. Smaller values are stricter.
 *     skip-verified-publishers: false  # If true, skip checks for verified publishers.
 *     check-against-verified-only: false  # If true, compare only against verified publishers' extensions.
 *     exclude-owner-namespaces: true  # If true, exclude namespaces where the publisher is an owner.
 */
@Configuration
public class SimilarityConfig {
    /**
     * If enabled, run similarity checks during extension publishing.
     *
     * Property: {@code ovsx.similarity.enabled}
     * Default: {@code false}
     */
    @Value("${ovsx.similarity.enabled:false}")
    private boolean enabled;

    /**
     * If enabled, only run similarity checks for the very first upload of an extension.
     * This means updates to an already existing extension (new versions) are not blocked by similarity.
     *
     * Property: {@code ovsx.similarity.new-extensions-only}
     * Default: {@code false}
     */
    @Value("${ovsx.similarity.new-extensions-only:false}")
    private boolean newExtensionsOnly;

    /**
     * Whether similarity failures are enforced (i.e. block publishing) when a similarity match is found.
     *
     * Why this exists:
     * - We sometimes want to run the check and store the audit trail (scan + failures)
     *   without rejecting publication (monitor-only mode).
     *
     * Default is true to preserve the historic behavior: when the similarity check is enabled,
     * it blocks publishing on matches unless explicitly configured otherwise.
     */
    @Value("${ovsx.similarity.enforced:true}")
    private boolean enforced;


    /**
     * Levenshtein threshold used to decide whether two extension identifiers are "too similar".
     * The check compares the edit distance against a fraction of the identifier length.
     * Smaller values are stricter. For example {@code 0.15} requires at least ~15% difference.
     *
     * Property: {@code ovsx.similarity.levenshtein-threshold}
     * Default: {@code 0.15}<br>
     * Valid range: {@code 0.0} - {@code 0.3} (validated at startup)
     */
    @Value("${ovsx.similarity.levenshtein-threshold:0.15}")
    private double levenshteinThreshold;

    /**
     * If enabled, do not run similarity checks for verified publishers.
     * This reduces friction for trusted publishers while keeping checks for unverified ones.
     *
     * Property: {@code ovsx.similarity.skip-verified-publishers}
     * Default: {@code false}
     */
    @Value("${ovsx.similarity.skip-verified-publishers:false}")
    private boolean skipVerifiedPublishers;

    /**
     * If enabled, compare new extensions only against extensions from verified publishers.
     * This reduces noise by focusing on protecting well-known publishers.
     *
     * Property: {@code ovsx.similarity.check-against-verified-only}
     * Default: {@code false}
     */
    @Value("${ovsx.similarity.check-against-verified-only:false}")
    private boolean checkAgainstVerifiedOnly;

    /**
     * If enabled, exclude namespaces where the publishing user is an owner from similarity checks.
     * This prevents false positives when a user legitimately controls multiple namespaces.
     *
     * Property: {@code ovsx.similarity.exclude-owner-namespaces}
     * Default: {@code true}
     */
    @Value("${ovsx.similarity.exclude-owner-namespaces:true}")
    private boolean excludeOwnerNamespaces;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNewExtensionsOnly() {
        return newExtensionsOnly;
    }
    
    public boolean isEnforced() {
        return enforced;
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