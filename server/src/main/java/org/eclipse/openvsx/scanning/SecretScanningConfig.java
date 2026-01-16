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
package org.eclipse.openvsx.scanning;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for secret scanning in extension packages.
 */
@Configuration
public class SecretScanningConfig {
    
    /**
     * Enables or disables secret scanning for extension publishing.
     *
     * Property: {@code ovsx.secret-scanning.enabled}
     * Default: {@code false}
     */
    @Value("${ovsx.secret-scanning.enabled:false}")
    private boolean enabled;

    /**
     * Automatically generate secret scanning rules from gitleaks.toml at startup.
     * 
     * When enabled, the application will download and convert the official gitleaks
     * configuration to YAML format at startup if the rules file does not exist.
     * 
     * Property: {@code ovsx.secret-scanning.auto-generate-rules}
     * Default: {@code false}
     */
    @Value("${ovsx.secret-scanning.auto-generate-rules:false}")
    private boolean autoGenerateRules;

    /**
     * Force regeneration of secret scanning rules even if they already exist.
     * 
     * Only has an effect when {@code auto-generate-rules} is enabled.
     * Use this to ensure rules are refreshed on every startup.
     *
     * Property: {@code ovsx.secret-scanning.force-regenerate-rules}
     * Default: {@code false}
     */
    @Value("${ovsx.secret-scanning.force-regenerate-rules:false}")
    private boolean forceRegenerateRules;

    /**
     * Full path where the auto-generated gitleaks rules will be written.
     * 
     * This should be an absolute path to a writable location. The directory
     * will be created automatically if it doesn't exist.
     * 
     * Only used when {@code auto-generate-rules} is enabled.
     * 
     * Property: {@code ovsx.secret-scanning.generated-rules-path}
     * Default: empty
     * Required when {@code auto-generate-rules} is true
     * Example: {@code /app/data/secret-scanning-rules-gitleaks.yaml}
     */
    @Value("${ovsx.secret-scanning.generated-rules-path:}")
    private String generatedRulesPath;

    /**
     * Whether secret scan findings are enforced (i.e. block publishing) when detected.
     *
     * Why this exists:
     * - We sometimes want to run secret scanning and record audit data,
     *   but not reject publication (monitor-only mode).
     *
     * Default is true to preserve historic behavior: when secret scanning is enabled,
     * findings will block publishing unless explicitly configured otherwise.
     */
    @Value("${ovsx.secret-scanning.enforced:true}")
    private boolean enforced;

    /**
     * Maximum file size to scan in bytes. Files larger than this are skipped.
     *
     * Property: {@code ovsx.secret-scanning.max-file-size-bytes}
     * Default: {@code 1048576} (1 MB)
     */
    @Value("${ovsx.secret-scanning.max-file-size-bytes:1048576}")
    private long maxFileSizeBytes;

    /**
     * Maximum line length to process in characters. Files with longer lines are skipped.
     * Very long lines (>10K) typically indicate minified/bundled code and may cause performance issues.
     *
     * Property: {@code ovsx.secret-scanning.max-line-length}
     * Default: {@code 10000}
     */
    @Value("${ovsx.secret-scanning.max-line-length:10000}")
    private int maxLineLength;

    /**
     * Comma-separated list of inline suppression markers that skip an entire line from scanning.
     *
     * Property: {@code ovsx.secret-scanning.inline-suppressions}
     * Default: empty
     * Example: {@code "secret-scanner:ignore,gitleaks:allow"}
     */
    @Value("${ovsx.secret-scanning.inline-suppressions:}")
    private String inlineSuppressionsString;


    /**
     * Timeout for scanning in seconds. If scanning takes longer, it will be aborted.
     *
     * Property: {@code ovsx.secret-scanning.timeout-seconds}
     * Default: {@code 5}
     */
    @Value("${ovsx.secret-scanning.timeout-seconds:5}")
    private int timeoutSeconds;
    
    /**
     * Maximum number of zip entries to inspect in an archive.
     *
     * Property: {@code ovsx.secret-scanning.max-entry-count}
     * Default: {@code 5000}
     */
    @Value("${ovsx.secret-scanning.max-entry-count:5000}")
    private int maxEntryCount;
    
    /**
     * Maximum total uncompressed bytes allowed across the archive.
     *
     * Property: {@code ovsx.secret-scanning.max-total-uncompressed-bytes}
     * Default: {@code 104857600} (100 MB)
     */
    @Value("${ovsx.secret-scanning.max-total-uncompressed-bytes:104857600}")
    private long maxTotalUncompressedBytes;
    
    /**
     * Maximum findings to collect before aborting to protect memory and UX.
     *
     * Property: {@code ovsx.secret-scanning.max-findings}
     * Default: {@code 200}
     */
    @Value("${ovsx.secret-scanning.max-findings:200}")
    private int maxFindings;

    /**
     * Comma-separated YAML rule file paths. Later entries override earlier by rule id.
     * Supports {@code classpath:} prefix for classpath resources.
     *
     * Property: {@code ovsx.secret-scanning.rules-path}
     * Default: empty
     */
    @Value("${ovsx.secret-scanning.rules-path:}")
    private String rulesPath;

    /**
     * How often (in lines) to check for scan timeout while reading files.
     *
     * Property: {@code ovsx.secret-scanning.timeout-check-every-n-lines}
     * Default: {@code 100}
     */
    @Value("${ovsx.secret-scanning.timeout-check-every-n-lines:100}")
    private int timeoutCheckEveryNLines;

    /**
     * Lines longer than this threshold without spaces are skipped to avoid minified blobs.
     *
     * Property: {@code ovsx.secret-scanning.long-line-no-space-threshold}
     * Default: {@code 1000}
     */
    @Value("${ovsx.secret-scanning.long-line-no-space-threshold:1000}")
    private int longLineNoSpaceThreshold;

    /**
     * Characters of context around a keyword when applying regex matching.
     *
     * Property: {@code ovsx.secret-scanning.keyword-context-chars}
     * Default: {@code 100}
     */
    @Value("${ovsx.secret-scanning.keyword-context-chars:100}")
    private int keywordContextChars;

    /**
     * Characters to include when logging secret preview values (for debugging allowlisted secrets).
     *
     * Property: {@code ovsx.secret-scanning.log-allowlisted-value-preview-length}
     * Default: {@code 10}
     */
    @Value("${ovsx.secret-scanning.log-allowlisted-value-preview-length:10}")
    private int logAllowlistedPreviewLength;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnforced() {
        return enforced;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Inline suppression markers that skip an entire line from scanning.
     */
    public @NotNull List<String> getInlineSuppressions() {
        if (inlineSuppressionsString == null || inlineSuppressionsString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(inlineSuppressionsString.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public int getMaxEntryCount() {
        return maxEntryCount;
    }
    
    public long getMaxTotalUncompressedBytes() {
        return maxTotalUncompressedBytes;
    }
    
    public int getMaxFindings() {
        return maxFindings;
    }

    /**
     * Get YAML rule paths, supporting comma-separated inputs.
     * Empty input yields an empty list so callers can fail fast.
     */
    public @NotNull List<String> getRulePaths() {
        if (rulesPath == null || rulesPath.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(rulesPath.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public int getTimeoutCheckEveryNLines() {
        return timeoutCheckEveryNLines;
    }

    public int getLongLineNoSpaceThreshold() {
        return longLineNoSpaceThreshold;
    }

    public int getKeywordContextChars() {
        return keywordContextChars;
    }

    public int getLogAllowlistedPreviewLength() {
        return logAllowlistedPreviewLength;
    }

    public boolean isAutoGenerateRules() {
        return autoGenerateRules;
    }

    public boolean isForceRegenerateRules() {
        return forceRegenerateRules;
    }

    public String getGeneratedRulesPath() {
        return generatedRulesPath;
    }

    @PostConstruct
    public void validate() {
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.max-file-size-bytes must be positive, got: " + maxFileSizeBytes);
        }
        
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.max-line-length must be positive, got: " + maxLineLength);
        }

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.timeout-seconds must be positive, got: " + timeoutSeconds);
        }
        
        if (maxEntryCount <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.max-entry-count must be positive, got: " + maxEntryCount);
        }
        
        if (maxTotalUncompressedBytes <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.max-total-uncompressed-bytes must be positive, got: " + maxTotalUncompressedBytes);
        }
        
        if (maxFindings <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.max-findings must be positive, got: " + maxFindings);
        }

        if (timeoutCheckEveryNLines <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.timeout-check-every-n-lines must be positive, got: " + timeoutCheckEveryNLines);
        }

        if (longLineNoSpaceThreshold <= 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.long-line-no-space-threshold must be positive, got: " + longLineNoSpaceThreshold);
        }

        if (keywordContextChars < 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.keyword-context-chars must be >= 0, got: " + keywordContextChars);
        }
        
        if (logAllowlistedPreviewLength < 0) {
            throw new IllegalArgumentException(
                "ovsx.secret-scanning.log-allowlisted-value-preview-length must be >= 0, got: " + logAllowlistedPreviewLength);
        }
    }
}

