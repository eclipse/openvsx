/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the lightweight wiring performed by {@link SecretScannerFactory}.
 * These tests avoid the Spring context and keep assertions close to the wiring logic.
 */
class SecretScannerFactoryTest {

    @Test
    void initialize_buildsEvenWhenDisabled() throws Exception {
        // Factory initializes if rule paths are configured, even when publishing-time scanning is disabled
        TrackingRuleLoader loader = new TrackingRuleLoader();
        SecretScanningConfiguration config = buildConfig(false);  // disabled
        setField(config, "rulesPath", "classpath:org/eclipse/openvsx/scanning/secret-rules-a.yaml");  // Use test resource
        MockGitleaksRulesGenerator generator = new MockGitleaksRulesGenerator(null);
        SecretScannerFactory factory = new SecretScannerFactory(loader, config, generator);

        factory.initialize();

        assertTrue(loader.wasCalled, "Loader should run when rule paths are configured");
        assertNotNull(factory.getScanner(), "Scanner should be created for retroactive scan use cases");
        assertEquals(1, factory.getRules().size(), "Rules should be loaded");
        assertFalse(factory.getKeywordToRules().isEmpty(), "Keyword index should be built");
    }

    @Test
    void initialize_skipsWhenNotConfigured() throws Exception {
        // Factory skips initialization if ovsx.secret-scanning block is not present
        TrackingRuleLoader loader = new TrackingRuleLoader();
        SecretScanningConfiguration config = buildConfig(false);  // disabled
        MockGitleaksRulesGenerator generator = new MockGitleaksRulesGenerator(null);
        SecretScannerFactory factory = new SecretScannerFactory(loader, config, generator);

        factory.initialize();

        assertFalse(loader.wasCalled, "Loader should not run when secret scanning is not configured");
        assertNull(factory.getScanner(), "Scanner should not be created when not configured");
        assertTrue(factory.getRules().isEmpty(), "Rules should remain empty");
        assertTrue(factory.getKeywordToRules().isEmpty(), "Keyword index should remain empty");
    }

    @Test
    void initialize_buildsMatchersAndIndexes() throws Exception {
        TrackingRuleLoader loader = new TrackingRuleLoader();
        SecretScanningConfiguration config = buildConfig(true);
        setField(config, "rulesPath",
                "classpath:org/eclipse/openvsx/scanning/secret-rules-a.yaml," +
                        "classpath:org/eclipse/openvsx/scanning/secret-rules-b.yaml");
        MockGitleaksRulesGenerator generator = new MockGitleaksRulesGenerator(null);

        SecretScannerFactory factory = new SecretScannerFactory(loader, config, generator);

        factory.initialize();

        assertTrue(loader.wasCalled, "Loader should run when scanning is enabled");
        assertNotNull(factory.getScanner(), "Scanner should be created from the loaded rules");
        assertEquals(2, factory.getRules().size(), "Expected merged rules from both YAML files");

        // Keyword index should include the lower-cased keyword from the override rule.
        assertTrue(factory.getKeywordToRules().containsKey("overridekey"));
        assertEquals("rule-a", factory.getKeywordToRules().get("overridekey").get(0).getId());

        // The matcher should actually find the keyword in a sample string.
        List<AhoCorasick.Match> matches = factory.getKeywordMatcher().search("prefix overridekey suffix");
        assertFalse(matches.isEmpty(), "Keyword matcher should find at least one match");
        assertEquals("overridekey", matches.get(0).getKeyword());
    }

    @Test
    void initialize_loadsGlobalAllowlistFromYaml() throws Exception {
        SecretRuleLoader loader = new SecretRuleLoader();
        SecretScanningConfiguration config = buildConfig(true);
        setField(config, "rulesPath", "classpath:org/eclipse/openvsx/scanning/secret-rules-with-allowlist.yaml");
        MockGitleaksRulesGenerator generator = new MockGitleaksRulesGenerator(null);

        SecretScannerFactory factory = new SecretScannerFactory(loader, config, generator);
        factory.initialize();

        assertNotNull(factory.getScanner(), "Scanner should be created");
        assertEquals(1, factory.getRules().size());
        
        // Verify scanner was initialized (global allowlist is used internally)
        // We can't directly inspect the scanner's internal allowlist, but we can verify it was created
        assertNotNull(factory.getScanner());
    }

    private SecretScanningConfiguration buildConfig(boolean enabled) throws Exception {
        // We set all the primitive fields explicitly so the factory can use getters safely.
        SecretScanningConfiguration config = new SecretScanningConfiguration();
        setField(config, "enabled", enabled);
        setField(config, "maxFileSizeBytes", 1024 * 1024);
        setField(config, "maxLineLength", 10_000);
        setField(config, "inlineSuppressionsString", "");
        setField(config, "timeoutSeconds", 5);
        setField(config, "maxEntryCount", 100);
        setField(config, "maxTotalUncompressedBytes", 1024 * 1024);
        setField(config, "maxFindings", 10);
        setField(config, "timeoutCheckEveryNLines", 10);
        setField(config, "longLineNoSpaceThreshold", 80);
        setField(config, "keywordContextChars", 10);
        setField(config, "logAllowlistedPreviewLength", 4);
        return config;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TrackingRuleLoader extends SecretRuleLoader {
        boolean wasCalled = false;

        @Override
        public LoadedRules loadAll(List<String> paths) {
            wasCalled = true;
            // Delegate to the real loader so the behavior stays realistic.
            return super.loadAll(paths);
        }
    }

    /**
     * Mock generator for testing factory integration.
     */
    static class MockGitleaksRulesGenerator extends GitleaksRulesGenerator {
        private final String mockPath;

        MockGitleaksRulesGenerator(String mockPath) {
            super(null);  // Don't need real config for mock
            this.mockPath = mockPath;
        }

        @Override
        public String getGeneratedRulesPath() {
            return mockPath;
        }
    }
}

