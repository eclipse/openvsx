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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.SizeLimitInputStream;
import jakarta.validation.constraints.NotNull;
import javax.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.google.re2j.Pattern;

/**
 * Scans individual files for secrets using keyword routing and regex validation.
 */
class SecretScanner {

    @FunctionalInterface
    interface FindingRecorder {
        boolean record(@NotNull List<SecretFinding> findings, @NotNull AtomicInteger count, @NotNull SecretFinding finding);
    }

    private static final Logger logger = LoggerFactory.getLogger(SecretScanner.class);
    private final AhoCorasick keywordMatcher;
    private final Map<String, List<SecretRule>> keywordToRules;
    private final List<SecretRule> rules;
    private final List<Pattern> globalAllowlistPatterns;
    private final List<Pattern> globalExcludedPathPatterns;
    private final AhoCorasick globalStopwordMatcher;
    private final AhoCorasick globalExcludedExtensionMatcher;
    private final AhoCorasick inlineSuppressionMatcher;
    private final EntropyCalculator entropyCalculator;
    private final long maxFileSizeBytes;
    private final int maxLineLength;
    private final int timeoutCheckEveryNLines;
    private final int longLineNoSpaceThreshold;
    private final int keywordContextChars;
    private final int logAllowlistedPreviewLength;

    SecretScanner(@NotNull AhoCorasick keywordMatcher,
                  @NotNull Map<String, List<SecretRule>> keywordToRules,
                  @NotNull List<SecretRule> rules,
                  @Nullable List<Pattern> allowlistPatterns,
                  @Nullable List<Pattern> excludedPathPatterns,
                  @Nullable AhoCorasick stopwordMatcher,
                  @Nullable AhoCorasick excludedExtensionMatcher,
                  @Nullable AhoCorasick inlineSuppressionMatcher,
                  @NotNull EntropyCalculator entropyCalculator,
                  long maxFileSizeBytes,
                  int maxLineLength,
                  int timeoutCheckEveryNLines,
                  int longLineNoSpaceThreshold,
                  int keywordContextChars,
                  int logAllowlistedPreviewLength) {
        this.keywordMatcher = keywordMatcher;
        this.keywordToRules = keywordToRules;
        this.rules = rules;
        this.globalAllowlistPatterns = allowlistPatterns != null ? allowlistPatterns : List.of();
        this.globalExcludedPathPatterns = excludedPathPatterns != null ? excludedPathPatterns : List.of();
        this.globalStopwordMatcher = stopwordMatcher;
        this.globalExcludedExtensionMatcher = excludedExtensionMatcher;
        this.inlineSuppressionMatcher = inlineSuppressionMatcher;
        this.entropyCalculator = entropyCalculator;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxLineLength = maxLineLength;
        this.timeoutCheckEveryNLines = timeoutCheckEveryNLines;
        this.longLineNoSpaceThreshold = longLineNoSpaceThreshold;
        this.keywordContextChars = keywordContextChars;
        this.logAllowlistedPreviewLength = logAllowlistedPreviewLength;
    }

    boolean scanFile(@NotNull ZipFile zipFile,
                     @NotNull ZipEntry entry,
                     @NotNull List<SecretFinding> findings,
                     long startTime,
                     long timeoutMillis,
                     @NotNull AtomicInteger findingsCount,
                     @NotNull FindingRecorder recorder) throws IOException {
        String filePath = entry.getName();

        if (Thread.currentThread().isInterrupted()) {
            return false;
        }

        if (!ArchiveUtil.isSafePath(filePath)) {
            return false;
        }

        if (entry.getSize() < 0 && entry.getCompressedSize() < 0) {
            return false;
        }

        if (entry.getSize() > maxFileSizeBytes) {
            return false;
        }
        
        // Check if the file type is excluded
        if (isExcludedFileType(filePath)) {
            return false;
        }

        // Check if the file is excluded by any of the global excluded patterns
        if (shouldExcludeFile(filePath)) {
            return false;
        }

        // Read the file line by line with a hard byte limit
        try (InputStream zipStream = zipFile.getInputStream(entry);
            // Use a limited stream since the entry header may not correctly reflect the file size
            InputStream limitedStream = new SizeLimitInputStream(zipStream, maxFileSizeBytes);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(limitedStream, StandardCharsets.UTF_8))) {

            int lineNumber = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (lineNumber % timeoutCheckEveryNLines == 0 && System.currentTimeMillis() - startTime > timeoutMillis) {
                    throw new SecretScanningTimeoutException("Secret scanning timed out during file: " + filePath);
                }

                lineNumber++;

                // Check if the line is too long
                if (line.length() > maxLineLength) {
                    continue;
                }
                
                // Check if the line is likely to be a minified/bundled file
                if (line.length() > longLineNoSpaceThreshold && !StringUtils.containsWhitespace(line)) {
                    continue;
                }

                // Check if the line is suppressed by the publisher
                if (hasInlineSuppression(line)) {
                    continue;
                }

                scanLineWithKeywordMatching(line, line.toLowerCase(), filePath, lineNumber, findings, findingsCount, recorder);
            }
        } catch (IOException e) {
            logger.warn("Error reading file {}: {}", filePath, e.getMessage());
            throw e;
        }

        return true;
    }

    /**
     * Scan the line with keyword matching.
     * This is a performance optimization to skip the regex matching for lines that don't contain any keywords.
     */
    private void scanLineWithKeywordMatching(@NotNull String line,
                                             @NotNull String lowerLine,
                                             @NotNull String filePath,
                                             int lineNumber,
                                             @NotNull List<SecretFinding> findings,
                                             @NotNull AtomicInteger findingsCount,
                                             @NotNull FindingRecorder recorder) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        List<AhoCorasick.Match> keywordMatches = keywordMatcher.search(lowerLine);

        Set<SecretRule> processedRules = new HashSet<>();

        for (AhoCorasick.Match match : keywordMatches) {
            String keyword = match.getKeyword();
            List<SecretRule> relevantRules = keywordToRules.get(keyword);

            if (relevantRules == null) {
                continue;
            }

            for (SecretRule rule : relevantRules) {
                if (processedRules.contains(rule)) {
                    continue;
                }
                processedRules.add(rule);

                int chunkStart = Math.max(0, match.getStartPos() - keywordContextChars);
                int chunkEnd = Math.min(line.length(), match.getEndPos() + keywordContextChars);
                String chunk = line.substring(chunkStart, chunkEnd);

                scanLineWithRule(rule, chunk, filePath, lineNumber, findings, findingsCount, recorder);
            }
        }

        for (SecretRule rule : rules) {
            if (rule.getKeywords().isEmpty() && !processedRules.contains(rule)) {
                scanLineWithRule(rule, line, filePath, lineNumber, findings, findingsCount, recorder);
            }
        }
    }

    /**
     * Scan the line with the given rule's regex pattern
     */
    private void scanLineWithRule(@NotNull SecretRule rule,
                                  @NotNull String chunk,
                                  @NotNull String filePath,
                                  int lineNumber,
                                  @NotNull List<SecretFinding> findings,
                                  @NotNull AtomicInteger findingsCount,
                                  @NotNull FindingRecorder recorder) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        var matcher = rule.getPattern().matcher(chunk);

        while (matcher.find()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            // Extract the secret value from the matched group
            int defaultGroup = matcher.groupCount() > 0 ? 1 : 0;
            int groupIndex = defaultGroup;
            Integer configuredGroup = rule.getSecretGroup();
            
            if (configuredGroup != null) {
                if (configuredGroup >= 0 && configuredGroup <= matcher.groupCount()) {
                    groupIndex = configuredGroup;
                }
            }

            String secretValue = matcher.group(groupIndex);

            if (secretValue == null) {
                continue;
            }

            // Check if the secret value is allowlisted by any of the rule's allowlist patterns
            if (!rule.getAllowlistPatterns().isEmpty()) {
                boolean allowlisted = false;
                for (Pattern allow : rule.getAllowlistPatterns()) {
                    if (allow.matcher(secretValue).find()) {
                        allowlisted = true;
                        break;
                    }
                }
                if (allowlisted) {
                    logger.debug("Skipping rule-allowlisted content in {}:{}: {}",
                            filePath, lineNumber, secretValue.substring(0, Math.min(logAllowlistedPreviewLength, secretValue.length())));
                    continue;
                }
            }

            // Check if the secret value is allowlisted by any of the global allowlist patterns
            if (isAllowlistedContent(secretValue)) {
                logger.debug("Skipping globally allowlisted content in {}:{}: {}",
                        filePath, lineNumber, secretValue.substring(0, Math.min(logAllowlistedPreviewLength, secretValue.length())));
                continue;
            }

            Double requiredEntropy = rule.getEntropy();
            double entropy = 0.0;

            // Check if the secret value is below the rule's required entropy threshold
            if (requiredEntropy != null) {
                entropy = entropyCalculator.calculate(secretValue);
                if (entropy < requiredEntropy) {
                    continue;
                }
            } else {
                entropy = entropyCalculator.calculate(secretValue);
            }

            logger.debug("Secret detected in {}:{} (rule: {}, entropy: {})",
                    filePath, lineNumber, rule.getId(), entropy);

            SecretFinding finding = new SecretFinding(filePath, lineNumber, entropy, secretValue, rule.getId());
            if (!recorder.record(findings, findingsCount, finding)) {
                return;
            }
        }
    }

    /**
     * Check if the line has any inline suppressions
     */
    private boolean hasInlineSuppression(@NotNull String line) {
        if (inlineSuppressionMatcher == null) {
            return false;
        }

        return !inlineSuppressionMatcher.search(line.toLowerCase()).isEmpty();
    }

    /**
     * Check if the file path is excluded by any of the global excluded path patterns
     */
    private boolean shouldExcludeFile(@NotNull String filePath) {
        for (Pattern pattern : globalExcludedPathPatterns) {
            if (pattern.matcher(filePath).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the file type is excluded by any of the global excluded file extensions
     */
    private boolean isExcludedFileType(@NotNull String filePath) {
        String lowerPath = filePath.toLowerCase();

        if (globalExcludedExtensionMatcher != null) {
            for (AhoCorasick.Match match : globalExcludedExtensionMatcher.search(lowerPath)) {
                if (match.getEndPos() == lowerPath.length()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if the secret value is allowlisted by any of the global allowlist patterns
     */
    private boolean isAllowlistedContent(@NotNull String secretValue) {
        // Stopwords are exact strings that should never be flagged as secrets
        if (globalStopwordMatcher != null && !globalStopwordMatcher.search(secretValue.toLowerCase()).isEmpty()) {
            return true;
        }

        // Allowlist patterns are regexes that may be case-sensitive, use original secretValue
        for (Pattern allowPattern : globalAllowlistPatterns) {
            if (allowPattern.matcher(secretValue).find()) {
                return true;
            }
        }

        return false;
    }
}

