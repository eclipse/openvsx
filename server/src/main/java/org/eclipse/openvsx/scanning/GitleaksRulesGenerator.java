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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates secret scanning rules from gitleaks.toml at application startup.
 * 
 * This component downloads the official gitleaks configuration and converts it to
 * the YAML format used by OpenVSX secret scanning. It runs before {@link SecretDetectorFactory}
 * to ensure rules are available when secret scanning initializes.
 * 
 * Generation is controlled by application configuration:
 * - ovsx.scanning.secret-scanning.enabled: Must be true to generate rules
 * - ovsx.scanning.secret-scanning.gitleaks.auto-fetch: Enable automatic generation (default: false)
 * - ovsx.scanning.secret-scanning.gitleaks.force-refresh: Force regeneration even if file exists (default: false)
 * 
 * Only loaded when gitleaks.auto-fetch is enabled via configuration.
 */
@Component
@ConditionalOnProperty(name = "ovsx.scanning.secret-scanning.gitleaks.auto-fetch", havingValue = "true")
public class GitleaksRulesGenerator {

    private static final Logger logger = LoggerFactory.getLogger(GitleaksRulesGenerator.class);
    
    private static final String GITLEAKS_URL = 
        "https://raw.githubusercontent.com/gitleaks/gitleaks/master/config/gitleaks.toml";
    
    /**
     * List of rule IDs to skip during conversion.
     * These rules produce too many false positives in the extension ecosystem.
     */
    private static final Set<String> SKIP_RULE_IDS = Set.of("generic-api-key");

    private final SecretDetectorConfig config;
    
    /**
     * Path to the generated rules file, if generation succeeded.
     * Other components can use this to load the generated file.
     */
    private String generatedRulesPath;

    public GitleaksRulesGenerator(SecretDetectorConfig config) {
        this.config = config;
    }
    
    /**
     * Get the path to the generated rules file, or null if not generated.
     * @return Absolute path to the generated file, or null if generation was skipped/failed
     */
    public String getGeneratedRulesPath() {
        return generatedRulesPath;
    }

    /**
     * Refresh gitleaks rules by downloading and regenerating.
     * 
     * This method is called by the scheduled refresh job to update rules.
     * Unlike {@link #generateRulesIfNeeded()}, this method always regenerates
     * the rules file, regardless of whether it already exists.
     * 
     * @return true if rules were successfully refreshed, false on error
     */
    public boolean refreshRules() {
        try {
            File outputFile = resolveOutputFile();
            
            if (outputFile == null) {
                logger.error("Cannot resolve output file for rules refresh");
                return false;
            }
            
            logger.info("Refreshing gitleaks rules from remote source...");
            generateRules(outputFile.toPath());
            
            if (!outputFile.exists() || outputFile.length() == 0) {
                logger.error("Rules refresh failed: output file is missing or empty");
                return false;
            }
            
            // Update the stored path in case it changed
            this.generatedRulesPath = outputFile.getAbsolutePath();
            
            logger.info("Successfully refreshed gitleaks rules: {} ({} bytes)", 
                outputFile.getName(), outputFile.length());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to refresh gitleaks rules", e);
            return false;
        }
    }

    /**
     * Generate gitleaks rules at startup if configured to do so.
     * 
     * Throws an exception if generation is enabled and fails, causing the application to fail to start.
     * This prevents the application from running with missing or invalid secret scanning rules.
     */
    @PostConstruct
    public void generateRulesIfNeeded() {
        // Skip if auto-fetch is disabled
        if (!config.isGitleaksAutoFetch()) {
            return;
        }

        try {
            File outputFile = resolveOutputFile();
            
            if (outputFile == null) {
                throw new IllegalStateException("Cannot resolve output file for generated rules");
            }
            
            // Store the path so other components can load it
            this.generatedRulesPath = outputFile.getAbsolutePath();
            
            // Check if file already exists
            if (outputFile.exists() && !config.isGitleaksForceRefresh()) {
                logger.info("Secret rules file already exists: {}", 
                    outputFile.getName());
                return;
            }

            generateRules(outputFile.toPath());
            
            // Verify the file was created successfully
            if (!outputFile.exists()) {
                throw new IllegalStateException(
                    "Failed to generate secret scanning rules file: " + outputFile.getAbsolutePath());
            }
            
            if (outputFile.length() == 0) {
                throw new IllegalStateException(
                    "Generated secret scanning rules file is empty: " + outputFile.getAbsolutePath());
            }
            
            logger.info("Generated secret scanning rules: {} ({} bytes)", 
                outputFile.getName(), outputFile.length());
            
        } catch (Exception e) {
            logger.error("Failed to generate secret scanning rules", e);
            throw new IllegalStateException(
                "Secret scanning rule generation failed. " +
                "Either fix the network/configuration issue or disable auto-generation " +
                "(set ovsx.secret-scanning.auto-fetch-gitleaks-rules=false)", e);
        }
    }

    /**
     * Main conversion logic: download, parse, and write YAML.
     */
    private void generateRules(Path outputPath) throws IOException, InterruptedException {
        // Download TOML
        logger.debug("Downloading gitleaks.toml from: {}", GITLEAKS_URL);
        String tomlContent = downloadGitleaksToml();
        
        GitleaksToml parsed = parseTomlWithJackson(tomlContent);
        
        List<Rule> rules = buildRules(parsed.rules);
        
        GlobalAllowlist allowlist = extractGlobalAllowlist(parsed.allowlist);
        
        GitleaksConfig configDto = new GitleaksConfig();
        configDto.rules = rules;
        configDto.allowlist = allowlist;
        
        writeYaml(configDto, outputPath);
    }

    /**
     * Download gitleaks.toml from the official repository.
     */
    private String downloadGitleaksToml() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITLEAKS_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download gitleaks.toml: HTTP " + response.statusCode());
        }
        
        String body = response.body();
        if (body == null || body.isEmpty()) {
            throw new IOException("Downloaded gitleaks.toml is empty");
        }
        
        return body;
    }

    /**
     * Parse TOML using Jackson's TomlMapper.
     */
    private GitleaksToml parseTomlWithJackson(String tomlContent) throws IOException {
        TomlMapper tomlMapper = new TomlMapper();
        return tomlMapper.readValue(tomlContent, GitleaksToml.class);
    }

    /**
     * Build rules from parsed TOML, filtering and normalizing.
     */
    private List<Rule> buildRules(List<RawRule> rawRules) {
        if (rawRules == null || rawRules.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Rule> result = new ArrayList<>();
        int skipped = 0;
        
        for (RawRule raw : rawRules) {
            // Skip rules known to cause false positives
            if (raw.id != null && SKIP_RULE_IDS.contains(raw.id)) {
                logger.debug("Skipping rule: {} (known to cause false positives)", raw.id);
                skipped++;
                continue;
            }
            
            Rule normalized = normalizeRule(raw);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        
        logger.info("Parsed {} rules (skipped {})", result.size(), skipped);
        return result;
    }

    /**
     * Extract global allowlist from parsed TOML.
     */
    private GlobalAllowlist extractGlobalAllowlist(RawAllowlist raw) {
        if (raw == null) {
            return new GlobalAllowlist();
        }
        
        GlobalAllowlist allowlist = new GlobalAllowlist();
        allowlist.paths = raw.paths != null ? raw.paths : new ArrayList<>();
        allowlist.regexes = raw.regexes != null ? raw.regexes : new ArrayList<>();
        allowlist.stopwords = raw.stopwords != null ? raw.stopwords : new ArrayList<>();
        allowlist.fileExtensions = raw.getFileExtensions() != null ? raw.getFileExtensions() : new ArrayList<>();
        
        return allowlist;
    }

    /**
     * Normalize a single gitleaks rule into the YAML DTO shape we load at runtime.
     */
    private Rule normalizeRule(RawRule raw) {
        if (raw == null || raw.id == null || raw.regex == null) {
            return null;
        }
        
        Rule rule = new Rule();
        rule.id = raw.id;
        rule.description = raw.description != null ? raw.description : "";
        rule.regex = raw.regex;
        rule.entropy = raw.entropy;
        rule.secretGroup = raw.secretGroup;
        
        // Normalize keywords to lowercase
        if (raw.keywords != null && !raw.keywords.isEmpty()) {
            rule.keywords = raw.keywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        } else {
            rule.keywords = new ArrayList<>();
        }
        
        // Collect allowlists (only regexes are supported)
        List<RawAllowlist> rawAllowlists = raw.getAllowlists();
        if (!rawAllowlists.isEmpty()) {
            rule.allowlists = new ArrayList<>();
            for (RawAllowlist rawAllowlist : rawAllowlists) {
                if (rawAllowlist.regexes != null && !rawAllowlist.regexes.isEmpty()) {
                    RuleAllowlist allowlist = new RuleAllowlist();
                    allowlist.regexes = rawAllowlist.regexes;
                    rule.allowlists.add(allowlist);
                }
            }
        }
        
        return rule;
    }

    /**
     * Write the configuration as YAML with proper formatting.
     */
    private void writeYaml(GitleaksConfig config, Path outputPath) throws IOException {
        // Add header comments manually
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Auto-generated at runtime by GitleaksRulesGenerator.java\n");
        yaml.append("# Do not edit this file manually; regenerate from gitleaks.toml instead.\n");
        yaml.append("\n");
        yaml.append("# Global allowlist - applies to all rules\n");
        yaml.append("allowlist:\n");
        
        // Render global allowlist with proper indentation (2 spaces for nested keys)
        if (!config.allowlist.paths.isEmpty()) {
            yaml.append("  paths:\n");
            for (String path : config.allowlist.paths) {
                yaml.append("    - ").append(quote(path)).append("\n");
            }
        }
        
        if (!config.allowlist.regexes.isEmpty()) {
            yaml.append("  regexes:\n");
            for (String regex : config.allowlist.regexes) {
                yaml.append("    - ").append(quote(regex)).append("\n");
            }
        }
        
        if (!config.allowlist.stopwords.isEmpty()) {
            yaml.append("  stopwords:\n");
            for (String word : config.allowlist.stopwords) {
                yaml.append("    - ").append(quote(word)).append("\n");
            }
        }
        
        if (!config.allowlist.fileExtensions.isEmpty()) {
            yaml.append("  file-extensions:\n");
            for (String ext : config.allowlist.fileExtensions) {
                yaml.append("    - ").append(quote(ext)).append("\n");
            }
        }
        
        // Render rules
        yaml.append("\nrules:\n");
        for (Rule rule : config.rules) {
            yaml.append("  - id: ").append(rule.id).append("\n");
            yaml.append("    description: ").append(quote(rule.description)).append("\n");
            yaml.append("    regex: ").append(quote(rule.regex)).append("\n");
            
            if (rule.entropy != null) {
                yaml.append("    entropy: ").append(rule.entropy).append("\n");
            }
            
            if (rule.secretGroup != null) {
                yaml.append("    secretGroup: ").append(rule.secretGroup).append("\n");
            }
            
            if (rule.keywords != null && !rule.keywords.isEmpty()) {
                yaml.append("    keywords:\n");
                for (String kw : rule.keywords) {
                    yaml.append("      - ").append(quote(kw)).append("\n");
                }
            } else {
                yaml.append("    keywords: []\n");
            }
            
            // Only include allowlists when present
            if (rule.allowlists != null && !rule.allowlists.isEmpty()) {
                yaml.append("    allowlists:\n");
                for (RuleAllowlist allowlist : rule.allowlists) {
                    if (allowlist.regexes != null && !allowlist.regexes.isEmpty()) {
                        yaml.append("      - regexes:\n");
                        for (String regex : allowlist.regexes) {
                            yaml.append("          - ").append(quote(regex)).append("\n");
                        }
                    }
                }
            }
        }
        
        Files.writeString(outputPath, yaml.toString());
    }

    /**
     * Quote a string for YAML if needed.
     * Uses JSON escaping which is compatible with YAML.
     */
    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        
        // Use JSON escaping which is compatible with YAML
        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            // Fallback to simple quoting
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    /**
     * Resolve the output file path for the generated rules.
     * 
     * Uses the path configured in ovsx.scanning.secret-scanning.gitleaks.output-path.
     * Fails fast with a clear error if not configured or not writable.
     */
    private File resolveOutputFile() {
        String path = config.getGitleaksOutputPath();
        
        // Fail fast if not configured
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException(
                "Secret scanning gitleaks.auto-fetch is enabled but 'ovsx.scanning.secret-scanning.gitleaks.output-path' is not configured. " +
                "Please set this property to the full path where rules should be written (e.g., /app/data/secret-scanning-rules-gitleaks.yaml)"
            );
        }
        
        File outputFile = new File(path);
        
        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            try {
                Files.createDirectories(parentDir.toPath());
                logger.debug("Created directory for generated rules: {}", parentDir.getAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Cannot create directory for generated rules: " + parentDir.getAbsolutePath() + 
                    ". Check file permissions and path configuration.", e);
            }
        }
        
        // Verify parent directory is writable
        if (parentDir != null && !parentDir.canWrite()) {
            throw new IllegalStateException(
                "Cannot write to directory for generated rules: " + parentDir.getAbsolutePath() + 
                ". Check file permissions.");
        }
        
        logger.debug("Using configured path for generated rules: {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    // ========================================================================================
    // Data structures for Jackson TOML parsing and YAML output
    // ========================================================================================
    
    /**
     * Root TOML structure as parsed by Jackson.
     * Ignores unknown properties like "title" that we don't need.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GitleaksToml {
        public List<RawRule> rules;
        public RawAllowlist allowlist;
    }
    
    /**
     * Raw rule as parsed from TOML (before normalization).
     * Ignores unknown properties that we don't use.
     * Gitleaks TOML can use either "allowlist" or "allowlists".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawRule {
        public String id;
        public String description;
        public String regex;
        public Double entropy;
        public Integer secretGroup;
        public List<String> keywords;
        public List<RawAllowlist> allowlist;
        public List<RawAllowlist> allowlists;
        
        /**
         * Helper to get allowlists as a list, checking both singular and plural forms.
         * There is the potential for migration from allowlist to allowlists according to gitleaks.toml.
         */
        List<RawAllowlist> getAllowlists() {
            if (allowlists != null && !allowlists.isEmpty()) {
                return allowlists;
            }
            if (allowlist != null && !allowlist.isEmpty()) {
                return allowlist;
            }
            return new ArrayList<>();
        }
    }

    /**
     * Raw allowlist from TOML (both global and rule-level).
     * Ignores unknown properties that we don't use.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawAllowlist {
        public List<String> paths;
        public List<String> regexes;
        public List<String> stopwords;
        public List<String> file_extensions;
        
        /**
         * Alias for Java code to use camelCase.
         */
        List<String> getFileExtensions() {
            return file_extensions;
        }
    }

    /**
     * Configuration container for output.
     */
    static class GitleaksConfig {
        List<Rule> rules;
        GlobalAllowlist allowlist;
    }

    /**
     * Normalized rule for output.
     */
    static class Rule {
        String id;
        String description;
        String regex;
        Double entropy;
        Integer secretGroup;
        List<String> keywords;
        List<RuleAllowlist> allowlists;
    }

    /**
     * Rule-level allowlist for output.
     */
    static class RuleAllowlist {
        List<String> regexes;
    }

    /**
     * Global allowlist configuration for output.
     */
    static class GlobalAllowlist {
        List<String> paths = new ArrayList<>();
        List<String> regexes = new ArrayList<>();
        List<String> stopwords = new ArrayList<>();
        List<String> fileExtensions = new ArrayList<>();
    }
}
