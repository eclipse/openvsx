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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing extension scans and recording artifacts.
 * 
 * Responsibilities:
 * - Owns the scan lifecycle state machine
 * - Delegates actual scanning to ExtensionScanner
 * - Records scan results and failures via ExtensionScanPersistenceService
 * - Provides query methods for scan data
 */
@Component
public class ExtensionScanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtensionScanService.class);

    private final ExtensionScanConfig config;
    private final ExtensionScanner scanner;
    private final ExtensionScanPersistenceService persistenceService;

    public ExtensionScanService(
            ExtensionScanConfig config,
            ExtensionScanner scanner,
            ExtensionScanPersistenceService persistenceService
    ) {
        this.config = config;
        this.scanner = scanner;
        this.persistenceService = persistenceService;
    }

    /**
     * Check if extension scanning is enabled.
     * @return true if scanning is enabled via configuration
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Creates a scan record from ExtensionProcessor metadata.
     * Use this when validation runs BEFORE extension creation.
     */
    public ExtensionScan initializeScan(ExtensionProcessor processor, UserData user) {
        return initializeScan(
            processor.getNamespace(),
            processor.getExtensionName(),
            processor.getVersion(),
            processor.getTargetPlatform(),
            processor.getDisplayName(),
            user
        );
    }

    private ExtensionScan initializeScan(
            String namespaceName,
            String extensionName,
            String version,
            String targetPlatform,
            String displayName,
            UserData user
    ) {
        logger.debug("Starting publish scan for {}.{} v{}", namespaceName, extensionName, version);
        
        return persistenceService.initializeScan(
            namespaceName,
            extensionName,
            version,
            targetPlatform,
            displayName,
            user
        );
    }

    /**
     * Run validation checks and record results.
     * 
     * Delegates the actual check execution to ExtensionScanner,
     * then records findings and manages state transitions.
     */
    public void runValidation(
            ExtensionScan scan,
            TempFile extensionFile,
            UserData user
    ) {
        transitionTo(scan, ScanStatus.VALIDATING);

        var scanResult = scanner.runValidationChecks(scan, extensionFile, user);

        if (scanResult.hasError()) {
            markScanAsErrored(scan, scanResult.getErrorMessage());
            throw new ErrorResultException(scanResult.getErrorMessage(), scanResult.error());
        }

        // Record all findings in the database.
        for (var finding : scanResult.findings()) {
            persistenceService.recordValidationFailure(
                scan,
                finding.checkType(),
                finding.ruleName(),
                finding.reason(),
                finding.enforced()
            );
        }

        // Handle enforced failures - block publication.
        if (scanResult.hasEnforcedFailure()) {
            transitionToTerminal(scan, ScanStatus.REJECTED);
            logger.info("Publication blocked due to policy violations: {}.{}",
                scan.getNamespaceName(), scan.getExtensionName());
            
            var enforcedFailures = scan.getValidationFailures()
                .stream()
                .filter(ExtensionValidationFailure::isEnforced)
                .toList();
            throw new ErrorResultException(getValidationFailuresErrorMessage(enforcedFailures));
        }

        if (!scanResult.getWarningFindings().isEmpty()) {
            logger.warn("Policy violations detected but not enforced: {}.{}",
                scan.getNamespaceName(), scan.getExtensionName());
        }

        logger.debug("Scan {} - Validation passed", scan.getId());
    }

    /**
     * Runs the long running scanning process for an extension.
     * 
     * Delegates the actual scan execution to ExtensionScanner,
     * then records findings and manages state transitions.
     */
    public void runScan(ExtensionScan scan, TempFile extensionFile, UserData user) {
        transitionTo(scan, ScanStatus.SCANNING);
        // TODO: Implement the scanning process
        scanner.runScanners(scan, extensionFile, user);
    }

    public void completeScanningSuccess(ExtensionScan scan) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping success marking",
                scan.getId(), scan.getStatus());
            return;
        }
        transitionToTerminal(scan, ScanStatus.PASSED);
    }

    public void quarantineScan(ExtensionScan scan) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping quarantine marking",
                scan.getId(), scan.getStatus());
            return;
        }
        transitionToTerminal(scan, ScanStatus.QUARANTINED);
    }

    public void markScanAsErrored(ExtensionScan scan, String errorMessage) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping error marking",
                scan.getId(), scan.getStatus());
            return;
        }
        validateTransition(scan.getStatus(), ScanStatus.ERRORED);
        persistenceService.markAsErrored(scan, errorMessage);
    }

    private void transitionTo(ExtensionScan scan, ScanStatus newStatus) {
        validateTransition(scan.getStatus(), newStatus);
        persistenceService.updateStatus(scan, newStatus);
    }
    
    private void transitionToTerminal(ExtensionScan scan, ScanStatus newStatus) {
        validateTransition(scan.getStatus(), newStatus);
        persistenceService.completeWithStatus(scan, newStatus);
    }
    
    private void validateTransition(ScanStatus from, ScanStatus to) {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateException(String.format(
                "Invalid scan state transition: %s -> %s", from, to
            ));
        }
    }
    
    private boolean isValidTransition(ScanStatus from, ScanStatus to) {
        if (from.isCompleted()) {
            return false;
        }
        
        return switch (from) {
            // Any in-progress state can end with an error.
            case STARTED -> to == ScanStatus.VALIDATING || to == ScanStatus.SCANNING || to == ScanStatus.ERRORED;
            case VALIDATING -> to == ScanStatus.SCANNING || to == ScanStatus.REJECTED || to == ScanStatus.ERRORED;
            case SCANNING -> to == ScanStatus.PASSED || to == ScanStatus.QUARANTINED || to == ScanStatus.ERRORED;
            default -> false;
        };
    }

    public void removeScan(ExtensionScan scan) {
        persistenceService.removeScan(scan);
    }

    private String getValidationFailuresErrorMessage(List<ExtensionValidationFailure> failures) {
        if (failures.isEmpty()) {
            return "Extension publication blocked.";
        }

        var failuresByType = failures.stream()
                .collect(Collectors.groupingBy(ExtensionValidationFailure::getCheckType));

        var parts = new java.util.ArrayList<String>();
        for (var entry : failuresByType.entrySet()) {
            var typeFailures = entry.getValue();

            int maxToShow = Math.min(3, typeFailures.size());
            var reasons = typeFailures.stream()
                    .limit(maxToShow)
                    .map(ExtensionValidationFailure::getValidationFailureReason)
                    .collect(Collectors.joining(", "));

            var part = new StringBuilder();
            part.append(reasons);
            if (typeFailures.size() > maxToShow) {
                part.append("  ... and ").append(typeFailures.size() - maxToShow).append(" more");
            }
            parts.add(part.toString());
        }

        return "Extension publication blocked: " + String.join(", ", parts) + ". For details on " + 
            "publishing extensions, see: https://github.com/eclipse/openvsx/wiki/Publishing-Extensions";
    }
}