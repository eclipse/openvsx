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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecretScanResult}.
 * These keep the simple value object behavior well defined and documented.
 */
class SecretScanResultTest {

    @Test
    void secretsFound_rejectsEmptyFindingList() {
        // We should never create a positive result with no findings.
        assertThrows(IllegalArgumentException.class, () -> SecretScanResult.secretsFound(List.of()));
    }

    @Test
    void secretsFound_wrapsFindingsImmutably() {
        // Build a single finding to ensure the result reflects its data.
        SecretFinding finding = new SecretFinding("file.txt", 2, 4.2, "supersecret", "rule-a");
        SecretScanResult result = SecretScanResult.secretsFound(List.of(finding));

        assertTrue(result.isSecretsFound());
        assertEquals(1, result.getFindings().size());
        assertEquals("Found 1 potential secret in extension package", result.getSummaryMessage());

        // Verify callers cannot mutate the internal list.
        assertThrows(UnsupportedOperationException.class, () -> result.getFindings().add(finding));
    }

    @Test
    void noSecretsFoundAndSkippedShareSafeDefaults() {
        // Both helpers should produce empty, immutable findings and the "no secrets" summary.
        List<SecretScanResult> results = List.of(
                SecretScanResult.noSecretsFound(),
                SecretScanResult.skipped()
        );

        for (SecretScanResult result : results) {
            assertFalse(result.isSecretsFound());
            assertTrue(result.getFindings().isEmpty());
            assertEquals("No secrets detected", result.getSummaryMessage());
            assertThrows(UnsupportedOperationException.class, () -> result.getFindings().add(
                    new SecretFinding("file", 1, 1.0, "value", "rule")));
        }
    }
}

