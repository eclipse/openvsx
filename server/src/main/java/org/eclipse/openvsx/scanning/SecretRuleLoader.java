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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.validation.constraints.NotNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads secret detection rules from a YAML file.
 */
@Component
public class SecretRuleLoader {

    private static final Logger logger = LoggerFactory.getLogger(SecretRuleLoader.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Container for loaded rules and global allowlist configuration.
     * 
     * This is returned by {@link #loadAll(List)} and contains:
     *   Compiled secret detection rules from all YAML files (deduplicated by ID)
     *   Global allowlist configuration from the last YAML file
     */
    public static class LoadedRules {
        private final List<SecretRule> rules;
        private final GlobalAllowlist globalAllowlist;

        /**
         * Create a container for loaded rules and global allowlist.
         */
        public LoadedRules(@NotNull List<SecretRule> rules, @Nullable GlobalAllowlist globalAllowlist) {
            this.rules = rules;
            this.globalAllowlist = globalAllowlist;
        }

        /**
         * Get the compiled secret detection rules.
         */
        public @NotNull List<SecretRule> getRules() {
            return rules;
        }

        /**
         * Get the global allowlist configuration.
         */
        public @Nullable GlobalAllowlist getGlobalAllowlist() {
            return globalAllowlist;
        }
    }

    /**
     * Load rules from a single path. Supports classpath: prefixed resources or absolute/relative file paths.
     */
    public List<SecretRule> load(@NotNull String path) {
        return loadAll(List.of(path)).getRules();
    }

    /**
     * Load rules and global allowlist from multiple YAML files.
     * Later files override earlier ones by rule id.
     * Global allowlists are merged from all files.
     */
    public LoadedRules loadAll(@NotNull List<String> paths) {
        if (paths.isEmpty()) {
            var message = "Secret scanning rules path list is empty";
            logger.warn(message);
            return new LoadedRules(List.of(), null);
        }

        Map<String, SecretRule> merged = new LinkedHashMap<>();
        List<String> allPaths = new ArrayList<>();
        List<String> allRegexes = new ArrayList<>();
        List<String> allStopwords = new ArrayList<>();
        List<String> allFileExtensions = new ArrayList<>();

        for (String path : paths) {
            RuleFileData loaded = loadSingle(path);
            for (SecretRule rule : loaded.rules) {
                // Last one wins to allow override behavior.
                merged.put(rule.getId(), rule);
            }
            // Merge global allowlist items from all files
            if (loaded.globalAllowlist != null) {
                if (loaded.globalAllowlist.paths != null) {
                    allPaths.addAll(loaded.globalAllowlist.paths);
                }
                if (loaded.globalAllowlist.regexes != null) {
                    allRegexes.addAll(loaded.globalAllowlist.regexes);
                }
                if (loaded.globalAllowlist.stopwords != null) {
                    allStopwords.addAll(loaded.globalAllowlist.stopwords);
                }
                if (loaded.globalAllowlist.fileExtensions != null) {
                    allFileExtensions.addAll(loaded.globalAllowlist.fileExtensions);
                }
            }
        }

        // Create combined global allowlist if any items were found
        GlobalAllowlist combinedAllowlist = null;
        if (!allPaths.isEmpty() || !allRegexes.isEmpty() || !allStopwords.isEmpty() || !allFileExtensions.isEmpty()) {
            combinedAllowlist = new GlobalAllowlist();
            combinedAllowlist.paths = allPaths;
            combinedAllowlist.regexes = allRegexes;
            combinedAllowlist.stopwords = allStopwords;
            combinedAllowlist.fileExtensions = allFileExtensions;
        }

        logger.info("Loaded {} rules from {} YAML files", merged.size(), paths.size());
        return new LoadedRules(List.copyOf(merged.values()), combinedAllowlist);
    }

    /**
     * Internal container for data loaded from a single YAML file.
     */
    private static class RuleFileData {
        final List<SecretRule> rules;
        final GlobalAllowlist globalAllowlist;

        RuleFileData(List<SecretRule> rules, GlobalAllowlist globalAllowlist) {
            this.rules = rules;
            this.globalAllowlist = globalAllowlist;
        }
    }

    private RuleFileData loadSingle(@NotNull String path) {
        // Fail when we cannot read rules and scanning is enabled.
        if (path.isBlank()) {
            var message = "Secret scanning rules path is empty";
            logger.error(message);
            throw new IllegalStateException(message);
        }

        try (InputStream is = openStream(path)) {
            if (is == null) {
                var message = "Secret scanning rules YAML not found at '" + path + "'";
                logger.error(message);
                throw new IllegalStateException(message);
            }

            RuleFile ruleFile = yamlMapper.readValue(is, RuleFile.class);
            if (ruleFile == null || ruleFile.rules == null || ruleFile.rules.isEmpty()) {
                var message = "Secret scanning rules YAML at '" + path + "' contained no rules";
                logger.error(message);
                throw new IllegalStateException(message);
            }

            // Parse rules
            List<SecretRule> result = new ArrayList<>();
            for (RuleDefinition def : ruleFile.rules) {
                if (def == null || def.id == null || def.regex == null) {
                    continue;
                }
                SecretRule.Builder builder = new SecretRule.Builder()
                        .id(def.id)
                        .description(def.description != null ? def.description : "")
                        .regex(def.regex);
                if (def.entropy != null) {
                    builder.entropy(def.entropy);
                }
                if (def.keywords != null && !def.keywords.isEmpty()) {
                    builder.keywords(def.keywords.toArray(new String[0]));
                }
                if (def.allowlists != null && !def.allowlists.isEmpty()) {
                    List<String> agg = new ArrayList<>();
                    for (Allowlist allow : def.allowlists) {
                        if (allow != null && allow.regexes != null) {
                            agg.addAll(allow.regexes);
                        }
                    }
                    if (!agg.isEmpty()) {
                        builder.allowlistRegexes(agg);
                    }
                }
                if (def.secretGroup != null) {
                    builder.secretGroup(def.secretGroup);
                }
                result.add(builder.build());
            }

            // Extract global allowlist if present
            GlobalAllowlist globalAllowlist = ruleFile.allowlist;

            logger.debug("Loaded {} rules from YAML {}", result.size(), path);
            if (globalAllowlist != null) {
                int pathCount = globalAllowlist.paths != null ? globalAllowlist.paths.size() : 0;
                int regexCount = globalAllowlist.regexes != null ? globalAllowlist.regexes.size() : 0;
                int stopwordCount = globalAllowlist.stopwords != null ? globalAllowlist.stopwords.size() : 0;
                int extensionCount = globalAllowlist.fileExtensions != null ? globalAllowlist.fileExtensions.size() : 0;
                logger.debug("Loaded global allowlist from YAML: {} paths, {} regexes, {} stopwords, {} file extensions",
                        pathCount, regexCount, stopwordCount, extensionCount);
            }

            return new RuleFileData(result, globalAllowlist);
        } catch (IOException e) {
            var message = "Failed to load secret scanning rules from YAML '" + path + "'";
            logger.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private @Nullable InputStream openStream(@NotNull String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(cp);
            if (!resource.exists()) {
                return null;
            }
            return resource.getInputStream();
        }
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            return null;
        }
        return new FileInputStream(f);
    }

    /**
     * DTO for YAML root structure.
     * 
     * allowlist:
     *   paths:
     *     - "\.test\."
     *   regexes:
     *     - "example"
     *   stopwords:
     *     - "placeholder"
     *   file-extensions:
     *     - ".png"
     * rules:
     *   - id: github-pat
     *     description: GitHub Personal Access Token
     *     regex: "ghp_[0-9a-zA-Z]{36}"
     *     keywords: ["github", "token"]
     */
    public static class RuleFile {
        /** List of secret detection rules */
        public List<RuleDefinition> rules;
        
        /** Global allowlist configuration that applies to all rules */
        public GlobalAllowlist allowlist;
    }

    /**
     * DTO for individual rule definitions in YAML.
     * 
     * Each rule defines how to detect a specific type of secret (API key, token, etc.).
     */
    public static class RuleDefinition {
        /** Unique rule identifier (required) */
        public String id;
        
        /** Human-readable description of what this rule detects */
        public String description;
        
        /** Regex pattern to match secrets (required, compiled as case-insensitive) */
        public String regex;
        
        /** Minimum Shannon entropy threshold (0.0-8.0). Matches below this are filtered out. */
        public Double entropy;
        
        /** Keywords that must appear in a line before applying regex (performance optimization) */
        public List<String> keywords;
        
        /** Rule-specific allowlist patterns to exclude known safe matches */
        public List<Allowlist> allowlists;
        
        /** Capture group index to extract the secret value (default: 1 if available, else 0) */
        public Integer secretGroup;
    }

    /**
     * DTO for rule-specific allowlist configuration.
     * 
     * Defines patterns that, when matched, cause a potential secret match to be excluded
     * as a known safe value.
     */
    public static class Allowlist {
        /** Regex patterns to exclude from this rule's matches (case-insensitive) */
        public List<String> regexes;
    }

    /**
     * DTO for global allowlist configuration.
     * 
     * Global allowlists apply to all rules and define:
     *
     *   paths: Regex patterns for file paths to exclude from scanning
     *   regexes: Regex patterns for content to exclude as known safe values
     *   stopwords: Exact strings to exclude (e.g., "example", "placeholder", "test")
     *   file-extensions: File extensions to exclude from scanning (e.g., ".png", ".jpg")
     *
     * These are loaded from the YAML files and merged with configuration from application.yml.
     */
    public static class GlobalAllowlist {
        /** Regex patterns for file paths to exclude (e.g., "node_modules/", "\.test\.") */
        public List<String> paths;
        
        /** Regex patterns for content to exclude as known safe (e.g., "^example$", "test.*") */
        public List<String> regexes;
        
        /** Exact strings to exclude (case-insensitive, e.g., "placeholder", "changeme") */
        public List<String> stopwords;
        
        /** File extensions to skip scanning (e.g., ".png", ".jpg", ".pdf") */
        @JsonProperty("file-extensions")
        public List<String> fileExtensions;
    }
}

