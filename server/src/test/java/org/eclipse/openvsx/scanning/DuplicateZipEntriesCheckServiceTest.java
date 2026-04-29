/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.openvsx.entities.ExtensionScan;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.util.TempFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for DuplicateZipEntriesCheckService.
 */
class DuplicateZipEntriesCheckServiceTest {

    @TempDir
    Path tempDir;

    private DuplicateZipEntriesCheckService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateZipEntriesCheckService();
    }

    @Test
    void check_passesForCleanZip() throws Exception {
        TempFile extensionFile = createTestZip("clean.vsix",
                "extension/package.json", "extension/README.md");

        var context = createContext(extensionFile);
        var result = service.check(context);

        assertTrue(result.passed());
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void check_failsWhenEntriesCollideAfterBackslashNormalization() throws Exception {
        // yauzl normalizes backslashes to forward slashes; these two entries collide
        // once normalized and signal a potentially malicious archive.
        TempFile extensionFile = createTestZip("duplicate.vsix",
                "extension/package.json", "extension\\package.json");

        var context = createContext(extensionFile);
        var result = service.check(context);

        assertFalse(result.passed());
        assertEquals(1, result.failures().size());
        assertEquals("DUPLICATE_NORMALIZED_ENTRIES", result.failures().getFirst().ruleName());
        assertTrue(result.failures().getFirst().reason().contains("duplicate zip entries"));
    }

    @Test
    void check_failsWhenEntriesCollideAfterDotSegmentNormalization() throws Exception {
        // extension/./package.json normalizes to extension/package.json
        TempFile extensionFile = createTestZip("dot-segment.vsix",
                "extension/package.json", "extension/./package.json");

        var context = createContext(extensionFile);
        var result = service.check(context);

        assertFalse(result.passed());
        assertEquals(1, result.failures().size());
        assertEquals("DUPLICATE_NORMALIZED_ENTRIES", result.failures().getFirst().ruleName());
    }

    @Test
    void check_failsWhenEntriesCollideAfterDotDotSegmentNormalization() throws Exception {
        // extension/sub/../package.json normalizes to extension/package.json
        TempFile extensionFile = createTestZip("dotdot-segment.vsix",
                "extension/package.json", "extension/sub/../package.json");

        var context = createContext(extensionFile);
        var result = service.check(context);

        assertFalse(result.passed());
        assertEquals(1, result.failures().size());
        assertEquals("DUPLICATE_NORMALIZED_ENTRIES", result.failures().getFirst().ruleName());
    }

    // --- Helper methods ---

    private TempFile createTestZip(String fileName, String... entryNames) throws Exception {
        Path zipPath = tempDir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (var name : entryNames) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(new byte[0]);
                zos.closeEntry();
            }
        }
        return new TempFile(zipPath);
    }

    private PublishCheck.Context createContext(TempFile extensionFile) {
        ExtensionScan scan = new ExtensionScan();
        scan.setNamespaceName("test-namespace");
        scan.setExtensionName("test-extension");
        scan.setExtensionVersion("1.0.0");

        UserData user = new UserData();
        user.setLoginName("testuser");

        return new PublishCheck.Context(scan, extensionFile, user);
    }
}
