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

import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TempFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Focused tests for "enforced vs monitor-only" behavior.
 *
 * Goal:
 * - failures are always persisted
 * - publishing is only blocked when enforcement is enabled (throws ErrorResultException)
 */
@ExtendWith(MockitoExtension.class)
class ExtensionScanServiceEnforcementTest {

    @Mock ExtensionScanConfig config;
    @Mock ExtensionScanner scanner;
    @Mock ExtensionScanPersistenceService persistenceService;
    @Mock TempFile extensionFile;

    private ExtensionScanService svc;
    private ExtensionScan scan;
    private UserData user;

    @BeforeEach
    void setUp() {
        svc = new ExtensionScanService(config, scanner, persistenceService);
        
        scan = new ExtensionScan();
        scan.setId(123);
        scan.setStatus(ScanStatus.STARTED);
        scan.setNamespaceName("ns");
        scan.setExtensionName("ext");
        scan.setExtensionVersion("1.0.0");
        scan.setTargetPlatform("universal");
        scan.setPublisher("publisher");

        user = new UserData();
        user.setLoginName("testuser");
        
        // Make mock persistence service actually update scan status for state machine validation
        lenient().doAnswer(invocation -> {
            ExtensionScan s = invocation.getArgument(0);
            ScanStatus status = invocation.getArgument(1);
            s.setStatus(status);
            return null;
        }).when(persistenceService).updateStatus(any(ExtensionScan.class), any(ScanStatus.class));
        
        lenient().doAnswer(invocation -> {
            ExtensionScan s = invocation.getArgument(0);
            ScanStatus status = invocation.getArgument(1);
            s.setStatus(status);
            return null;
        }).when(persistenceService).completeWithStatus(any(ExtensionScan.class), any(ScanStatus.class));
        
        // Make mock persistence service actually add failures to scan
        lenient().doAnswer(invocation -> {
            ExtensionScan s = invocation.getArgument(0);
            String checkType = invocation.getArgument(1);
            String ruleName = invocation.getArgument(2);
            String reason = invocation.getArgument(3);
            boolean enforced = invocation.getArgument(4);
            
            var failure = ExtensionValidationFailure.create(checkType, ruleName, reason);
            failure.setEnforced(enforced);
            s.addValidationFailure(failure);
            return null;
        }).when(persistenceService).recordValidationFailure(any(), any(), any(), any(), anyBoolean());
    }

    // ========== HAPPY PATH TESTS ==========

    @Test
    void runValidation_passes_whenAllChecksPass() {
        // Scanner returns no findings
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(List.of(), false, null, null));

        // Act - should not throw
        svc.runValidation(scan, extensionFile, user);

        // Assert: no failures recorded, validation lifecycle methods called
        verify(persistenceService).updateStatus(scan, ScanStatus.VALIDATING);
        verify(persistenceService, never()).recordValidationFailure(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void runValidation_delegatesToScanner() {
        // Scanner returns pass
        when(scanner.runValidationChecks(eq(scan), eq(extensionFile), eq(user)))
            .thenReturn(new ExtensionScanner.ScanResult(List.of(), false, null, null));

        // Act
        svc.runValidation(scan, extensionFile, user);

        // Assert: scanner was called with correct arguments
        verify(scanner).runValidationChecks(scan, extensionFile, user);
    }

    // ========== ENFORCEMENT TESTS ==========

    @Test
    void runValidation_doesNotThrow_whenFailuresNotEnforced_butPersistsFailures() {
        // Scanner returns failures but not enforced
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", false),
            new ExtensionScanner.CheckFinding("CHECK_2", "rule2", "reason2", false)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, false, null, null));

        // Act: should NOT throw because nothing is enforced
        svc.runValidation(scan, extensionFile, user);

        // Assert: failures persisted even in monitor-only mode
        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_1"), eq("rule1"), eq("reason1"), eq(false));
        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_2"), eq("rule2"), eq("reason2"), eq(false));
    }

    @Test
    void runValidation_throwsWhenEnforcedCheckFails() {
        // Scanner returns enforced failure
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", true)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, true, null, null));

        // Act & Assert: should throw ErrorResultException
        assertThatThrownBy(() -> svc.runValidation(scan, extensionFile, user))
            .isInstanceOf(ErrorResultException.class);

        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_1"), any(), any(), eq(true));
    }

    @Test
    void runValidation_throwsWhenBothEnforced_andBothFail() {
        // Scanner returns multiple enforced failures
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", true),
            new ExtensionScanner.CheckFinding("CHECK_2", "rule2", "reason2", true)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, true, null, null));

        // Act & Assert
        assertThatThrownBy(() -> svc.runValidation(scan, extensionFile, user))
            .isInstanceOf(ErrorResultException.class);

        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_1"), any(), any(), eq(true));
        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_2"), any(), any(), eq(true));
    }

    @Test
    void runValidation_throwsWhenOneEnforcedFails_andOneNotEnforcedFails() {
        // Mixed: one enforced failure, one non-enforced warning
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", true),
            new ExtensionScanner.CheckFinding("CHECK_2", "rule2", "reason2", false)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, true, null, null));

        // Act & Assert: blocked because at least one enforced check failed
        assertThatThrownBy(() -> svc.runValidation(scan, extensionFile, user))
            .isInstanceOf(ErrorResultException.class);

        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_1"), any(), any(), eq(true));
        verify(persistenceService).recordValidationFailure(eq(scan), eq("CHECK_2"), any(), any(), eq(false));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void runValidation_marksErrorAndRethrows_whenScannerReturnsError() {
        var exception = new RuntimeException("Check failed unexpectedly");
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(List.of(), false, exception, "CHECK_1"));

        // Act & Assert
        assertThatThrownBy(() -> svc.runValidation(scan, extensionFile, user))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Check failed unexpectedly");

        verify(persistenceService).markAsErrored(eq(scan), anyString());
    }

    // ========== LIFECYCLE TESTS ==========

    @Test
    void runValidation_transitionsToValidating() {
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(List.of(), false, null, null));

        // Act
        svc.runValidation(scan, extensionFile, user);

        // Assert: transitions to VALIDATING
        verify(persistenceService).updateStatus(scan, ScanStatus.VALIDATING);
    }

    @Test
    void runValidation_completesNormally_whenFailuresNotEnforced() {
        // Scanner returns non-enforced failures (warnings)
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", false),
            new ExtensionScanner.CheckFinding("CHECK_2", "rule2", "reason2", false)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, false, null, null));

        // Act - should not throw
        svc.runValidation(scan, extensionFile, user);

        // Assert: completes normally (no exception)
        verify(persistenceService).updateStatus(scan, ScanStatus.VALIDATING);
    }

    @Test
    void runValidation_transitionsToRejected_whenEnforcedFailure() {
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", true)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, true, null, null));

        // Act & Assert: throws and transitions to REJECTED
        assertThatThrownBy(() -> svc.runValidation(scan, extensionFile, user))
            .isInstanceOf(ErrorResultException.class);

        verify(persistenceService).completeWithStatus(scan, ScanStatus.REJECTED);
    }

    // ========== MULTIPLE FINDINGS TESTS ==========

    @Test
    void runValidation_recordsAllFindings() {
        // Scanner returns multiple findings from one check type
        var findings = List.of(
            new ExtensionScanner.CheckFinding("CHECK_1", "rule1", "reason1", false),
            new ExtensionScanner.CheckFinding("CHECK_1", "rule2", "reason2", false),
            new ExtensionScanner.CheckFinding("CHECK_1", "rule3", "reason3", false)
        );
        when(scanner.runValidationChecks(any(), any(), any()))
            .thenReturn(new ExtensionScanner.ScanResult(findings, false, null, null));

        // Act
        svc.runValidation(scan, extensionFile, user);

        // Assert: all 3 findings recorded
        verify(persistenceService, times(3)).recordValidationFailure(eq(scan), eq("CHECK_1"), any(), any(), eq(false));
    }
}
