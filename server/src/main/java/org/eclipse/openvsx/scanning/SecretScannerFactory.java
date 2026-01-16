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

import com.google.re2j.Pattern;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds and wires the secret-scanning primitives once at startup.
 * 
 * The factory only initializes if rule paths are configured (via {@code rules-path})
 * or auto-generation is enabled (via {@code auto-generate-rules}).
 * If neither is configured, initialization is skipped, allowing the application
 * to start without requiring secret scanning infrastructure.
 * 
 * If auto-generation is enabled, this depends on {@link GitleaksRulesGenerator} 
 * to ensure rules are generated before we try to load them.
 * Only loaded when secret scanning is enabled via configuration.
 */
@Component
@ConditionalOnProperty(name = "ovsx.secret-scanning.enabled", havingValue = "true")
public class SecretScannerFactory {

    private static final Logger logger = LoggerFactory.getLogger(SecretScannerFactory.class);

    private final SecretRuleLoader ruleLoader;
    private final SecretScanningConfig config;
    private final GitleaksRulesGenerator generator;

    private List<SecretRule> rules = List.of();
    private Map<String, List<SecretRule>> keywordToRules = Map.of();
    private AhoCorasick keywordMatcher = new AhoCorasick();
    private SecretScanner scanner;

    public SecretScannerFactory(
            @NotNull SecretRuleLoader ruleLoader, 
            @NotNull SecretScanningConfig config,
            @Nullable GitleaksRulesGenerator generator) {
        this.ruleLoader = ruleLoader;
        this.config = config;
        this.generator = generator;
    }

    @PostConstruct
    public void initialize() {
        // Build rules path list, prepending the generated file if auto-generation is enabled
        List<String> rulePaths = buildRulePaths();
        
        // Skip initialization if there are no rule paths to load
        if (rulePaths.isEmpty()) {
            logger.info("No secret scanning rules configured; skipping scanner initialization");
            return;
        }
        
        // Load all rules from the rule paths
        SecretRuleLoader.LoadedRules loaded = ruleLoader.loadAll(rulePaths);
        List<SecretRule> loadedRules = loaded.getRules();
        SecretRuleLoader.GlobalAllowlist globalAllowlist = loaded.getGlobalAllowlist();

        // Build the keyword index for efficient keyword matching based on all loaded rules
        Set<String> allKeywords = new HashSet<>();
        Map<String, List<SecretRule>> keywordIndex = buildKeywordIndex(loadedRules, allKeywords);
        AhoCorasick builtKeywordMatcher = buildMatcher(allKeywords);

        // Build the global excluded path patterns for path matching based on the global allowlist
        List<Pattern> globalExcludedPathPatterns = getGlobalExcludedPathPatterns(globalAllowlist, config);
        List<Pattern> globalAllowlistPatterns = getGlobalAllowlistPatterns(globalAllowlist, config);
        List<String> globalStopwords = getGlobalStopwords(globalAllowlist, config);
        List<String> globalExcludedExtensions = getGlobalExcludedExtensions(globalAllowlist, config);

        // Build the matchers for global stopwords, excluded extensions, and inline suppressions
        AhoCorasick globalStopwordMatcher = buildMatcher(globalStopwords);
        AhoCorasick globalExcludedExtensionMatcher = buildMatcher(globalExcludedExtensions);
        
        List<String> inlineSuppressionsLower = getInlineSuppressions(config);
        AhoCorasick inlineSuppressionMatcher = buildMatcher(inlineSuppressionsLower);

        // Build the entropy calculator for entropy calculation
        EntropyCalculator entropyCalculator = new EntropyCalculator();

        this.rules = List.copyOf(loadedRules);
        this.keywordToRules = keywordIndex;
        this.keywordMatcher = builtKeywordMatcher != null ? builtKeywordMatcher : new AhoCorasick();
        this.scanner = new SecretScanner(
                this.keywordMatcher,
                this.keywordToRules,
                this.rules,
                globalAllowlistPatterns,
                globalExcludedPathPatterns,
                globalStopwordMatcher,
                globalExcludedExtensionMatcher,
                inlineSuppressionMatcher,
                entropyCalculator,
                config.getMaxFileSizeBytes(),
                config.getMaxLineLength(),
                config.getTimeoutCheckEveryNLines(),
                config.getLongLineNoSpaceThreshold(),
                config.getKeywordContextChars(),
                config.getLogAllowlistedPreviewLength()
        );

        logger.info("Secret scanning initialized: {} rules loaded, {} unique keywords indexed",
                this.rules.size(), this.keywordToRules.size());
    }

    public @Nullable SecretScanner getScanner() {
        return scanner;
    }

    public @NotNull List<SecretRule> getRules() {
        return rules;
    }

    public @NotNull Map<String, List<SecretRule>> getKeywordToRules() {
        return keywordToRules;
    }

    public @NotNull AhoCorasick getKeywordMatcher() {
        return keywordMatcher;
    }

    private Map<String, List<SecretRule>> buildKeywordIndex(@NotNull List<SecretRule> sourceRules, @NotNull Set<String> allKeywords) {
        Map<String, List<SecretRule>> index = new HashMap<>();

        for (SecretRule rule : sourceRules) {
            for (String keyword : rule.getKeywords()) {
                allKeywords.add(keyword);
                index.computeIfAbsent(keyword, k -> new ArrayList<>()).add(rule);
            }
        }

        logger.debug("Built keyword index with {} keywords from {} rules", allKeywords.size(), sourceRules.size());
        return index;
    }

    /**
     * Build the Aho-Corasick matcher for the given keywords
     */
    private AhoCorasick buildMatcher(java.util.Collection<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        Set<String> normalized = new HashSet<>();
        for (String keyword : keywords) {
            if (keyword == null) {
                continue;
            }
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toLowerCase());
        }

        if (normalized.isEmpty()) {
            return null;
        }

        AhoCorasick matcher = new AhoCorasick();
        matcher.build(normalized);
        return matcher;
    }

    /**
     * Get global excluded file extensions from YAML allowlist or fall back to config.
     * Combines extensions from the global allowlist in the YAML with extensions from config.
     */
    private List<String> getGlobalExcludedExtensions(
            @Nullable SecretRuleLoader.GlobalAllowlist globalAllowlist,
            @NotNull SecretScanningConfig config) {
        List<String> result = new ArrayList<>();

        if (globalAllowlist != null && globalAllowlist.fileExtensions != null) {
            result.addAll(globalAllowlist.fileExtensions.stream()
                    .map(String::toLowerCase)
                    .toList());
        }

        return result;
    }

    /**
     * Get global excluded path patterns from YAML allowlist or fall back to config.
     * Paths are compiled as regex patterns for flexible matching.
     * Combines paths from the global allowlist in the YAML with paths from config.
     */
    private List<Pattern> getGlobalExcludedPathPatterns(
            @Nullable SecretRuleLoader.GlobalAllowlist globalAllowlist,
            @NotNull SecretScanningConfig config) {
        List<Pattern> result = new ArrayList<>();

        if (globalAllowlist != null && globalAllowlist.paths != null) {
            result.addAll(globalAllowlist.paths.stream()
                    .map(path -> Pattern.compile(path, Pattern.CASE_INSENSITIVE))
                    .toList());
        }

        return result;
    }

    /**
     * Get global allowlist patterns from YAML or fall back to config.
     * Uses regex patterns from the global allowlist in the YAML, falling back to config.
     */
    private List<Pattern> getGlobalAllowlistPatterns(
            @Nullable SecretRuleLoader.GlobalAllowlist globalAllowlist,
            @NotNull SecretScanningConfig config) {
        List<Pattern> result = new ArrayList<>();

        if (globalAllowlist != null && globalAllowlist.regexes != null && !globalAllowlist.regexes.isEmpty()) {
            result.addAll(globalAllowlist.regexes.stream()
                    .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                    .toList());
        }

        return result;
    }

    /**
     * Get global stopwords from YAML or fall back to config.
     * Uses stopwords from the global allowlist in the YAML, falling back to config.
     */
    private List<String> getGlobalStopwords(
            @Nullable SecretRuleLoader.GlobalAllowlist globalAllowlist,
            @NotNull SecretScanningConfig config) {
        List<String> result = new ArrayList<>();

        if (globalAllowlist != null && globalAllowlist.stopwords != null) {
            result.addAll(globalAllowlist.stopwords.stream()
                    .map(String::toLowerCase)
                    .toList());
        }

        return result;
    }

    /**
     * Get inline suppression markers from config.
     * Returns lowercase versions of the suppression markers for case-insensitive matching.
     */
    private List<String> getInlineSuppressions(@NotNull SecretScanningConfig config) {
        return config.getInlineSuppressions().stream()
                .map(String::toLowerCase)
                .toList();
    }

    /**
     * Build the list of rule paths to load.
     * If auto-generation is enabled and succeeded, prepend the generated file path.
     * Append the configured paths from the application.yml.
     */
    private List<String> buildRulePaths() {
        List<String> paths = new ArrayList<>();
        
        // If auto-generation is enabled and the generator bean exists, use the generated file
        if (generator != null) {
            String generatedPath = generator.getGeneratedRulesPath();
            if (generatedPath != null) {
                logger.debug("Using auto-generated rules file: {}", generatedPath);
                paths.add(generatedPath);
            }
        }
        
        // Add configured paths (these may include custom rules or override gitleaks rules)
        paths.addAll(config.getRulePaths());
        
        return paths;
    }
}


