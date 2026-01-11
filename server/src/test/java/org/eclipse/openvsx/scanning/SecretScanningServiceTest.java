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
import org.eclipse.openvsx.util.ArchiveUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for guardrails and helpers inside SecretScanningService.
 * We use reflection to keep production visibility unchanged while ensuring coverage.
 */
class SecretScanningServiceTest {

    @Test
    void enforceArchiveLimits_throwsWhenEntryCountExceeded() {
        List<ZipEntry> entries = List.of(new ZipEntry("a"), new ZipEntry("b"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ArchiveUtil.enforceArchiveLimits(entries, 1, 10_000));
        assertTrue(ex.getMessage().contains("too many entries"));
    }

    @Test
    void enforceArchiveLimits_throwsWhenTotalSizeExceeded() {
        ZipEntry e1 = new ZipEntry("a");
        e1.setSize(6);
        ZipEntry e2 = new ZipEntry("b");
        e2.setSize(6);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ArchiveUtil.enforceArchiveLimits(List.of(e1, e2), 10, 10));
        assertTrue(ex.getMessage().contains("Uncompressed archive size exceeds"));
    }

    @Test
    void isSafePath_rejectsTraversalAndAbsolute() {
        assertFalse(ArchiveUtil.isSafePath("../evil"));
        assertFalse(ArchiveUtil.isSafePath("/abs/path"));
    }

    @Test
    void isSafePath_allowsNormalRelative() {
        assertTrue(ArchiveUtil.isSafePath("folder/file.txt"));
    }

    @Test
    void recordFinding_respectsGlobalCap() throws Exception {
        SecretScanningService service = buildServiceWithLimits(10, 100, 1);
        List<SecretFinding> findings = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);

        SecretFinding first = new SecretFinding(
                "a.txt", 1, 4.0, "secretvalue", "rule1");
        SecretFinding second = new SecretFinding(
                "b.txt", 2, 4.0, "secretvalue2", "rule2");

        assertTrue(invokeRecordFinding(service, findings, count, first));
        assertEquals(1, findings.size());
        assertThrows(SecretScanningService.ScanCancelledException.class,
                () -> invokeRecordFinding(service, findings, count, second));
        assertEquals(1, findings.size());
    }

    @Test
    void loadRules_failsFastWhenMissing() {
        var loader = new SecretRuleLoader();
        assertThrows(IllegalStateException.class, () -> loader.load("non-existent-rules.yaml"));
    }

    // --- Helpers ----------------------------------------------------------------

    private SecretScanningService buildServiceWithLimits(int maxEntries, long maxBytes, int maxFindings) throws Exception {
        SecretScanningConfig config = new SecretScanningConfig();
        // Disable scanning so we can construct the service without loading rule files.
        setField(config, "enabled", false);
        setField(config, "maxFileSizeBytes", 1024 * 1024);
        setField(config, "maxLineLength", 10_000);
        setField(config, "timeoutSeconds", 10);
        setField(config, "maxEntryCount", maxEntries);
        setField(config, "maxTotalUncompressedBytes", maxBytes);
        setField(config, "maxFindings", maxFindings);
        setField(config, "rulesPath", "classpath:org/eclipse/openvsx/scanning/secret-rules-a.yaml");
        setField(config, "timeoutCheckEveryNLines", 100);
        setField(config, "longLineNoSpaceThreshold", 1000);
        setField(config, "keywordContextChars", 100);
        setField(config, "logAllowlistedPreviewLength", 10);

        SecretRuleLoader loader = new SecretRuleLoader();
        var generator = new GitleaksRulesGenerator(config);
        SecretScannerFactory factory = new SecretScannerFactory(loader, config, generator);
        factory.initialize(); // manually trigger wiring outside of Spring context for the test
        var executor = new org.springframework.core.task.SimpleAsyncTaskExecutor();
        return new SecretScanningService(config, factory, executor);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private boolean invokeRecordFinding(SecretScanningService service,
                                        List<SecretFinding> findings,
                                        AtomicInteger count,
                                        SecretFinding finding) throws Exception {
        Method m = SecretScanningService.class.getDeclaredMethod(
                "recordFinding", List.class, AtomicInteger.class, SecretFinding.class);
        m.setAccessible(true);
        try {
            return (boolean) m.invoke(service, findings, count, finding);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(cause);
        }
    }
}

