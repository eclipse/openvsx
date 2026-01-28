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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.ScannerJobRepository;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing extension scans.
 * 
 * Owns scan lifecycle (STARTED → VALIDATING → SCANNING → PASSED/QUARANTINED),
 * runs validation checks, and submits scanner jobs via JobRunr.
 */
@Component
public class ExtensionScanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtensionScanService.class);

    private final ExtensionScanConfig config;
    private final PublishCheckRunner checkRunner;
    private final ExtensionScanPersistenceService persistenceService;
    private final ScannerRegistry scannerRegistry;
    private final JobRequestScheduler jobScheduler;
    private final ScannerJobRepository scanJobRepository;

    public ExtensionScanService(
            ExtensionScanConfig config,
            PublishCheckRunner checkRunner,
            ExtensionScanPersistenceService persistenceService,
            ScannerRegistry scannerRegistry,
            JobRequestScheduler jobScheduler,
            ScannerJobRepository scanJobRepository
    ) {
        this.config = config;
        this.checkRunner = checkRunner;
        this.persistenceService = persistenceService;
        this.scannerRegistry = scannerRegistry;
        this.jobScheduler = jobScheduler;
        this.scanJobRepository = scanJobRepository;
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
    @NonNull
    public ExtensionScan initializeScan(@NonNull ExtensionProcessor processor, @NonNull UserData user) {
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
            @NonNull ExtensionScan scan,
            @NonNull TempFile extensionFile,
            @NonNull UserData user
    ) {
        transitionTo(scan, ScanStatus.VALIDATING);

        var checkResult = checkRunner.runChecks(scan, extensionFile, user);

        // Record ALL check executions for audit trail (pass, fail, skip, error).
        // This gives admins visibility into what checks were run.
        for (var execution : checkResult.checkExecutions()) {
            persistenceService.recordCheckResult(
                scan,
                execution.checkType(),
                ScanCheckResult.CheckCategory.PUBLISH_CHECK,
                execution.result(),
                execution.startedAt(),
                execution.completedAt(),
                null,  // filesScanned - not applicable for publish checks
                execution.findingsCount(),
                execution.summary(),
                execution.errorMessage(),
                null   // scannerJobId - not applicable for publish checks
            );
        }

        if (checkResult.hasError()) {
            markScanAsErrored(scan, checkResult.getErrorMessage());
            throw new ErrorResultException(checkResult.getErrorMessage(), checkResult.error());
        }

        // Record all findings in the database (detailed failure records).
        for (var finding : checkResult.findings()) {
            persistenceService.recordValidationFailure(
                scan,
                finding.checkType(),
                finding.ruleName(),
                finding.reason(),
                finding.enforced()
            );
        }

        // Handle enforced failures - block publication.
        if (checkResult.hasEnforcedFailure()) {
            transitionToTerminal(scan, ScanStatus.REJECTED);
            logger.info("Publication blocked due to policy violations: {}.{}",
                scan.getNamespaceName(), scan.getExtensionName());
            
            // Use user-facing messages from findings (which may be masked for security).
            // Detailed reasons are still stored in the database for admin review.
            var enforcedFindings = checkResult.getEnforcedFindings();
            throw new ErrorResultException(getUserFacingErrorMessage(enforcedFindings));
        }

        if (!checkResult.getWarningFindings().isEmpty()) {
            logger.warn("Policy violations detected but not enforced: {}.{}",
                scan.getNamespaceName(), scan.getExtensionName());
        }

        logger.debug("Scan {} - Validation passed", scan.getId());
    }

    /**
     * Submit long-running scanner jobs for an extension version.
     * 
     * This method:
     * 1. Gets all registered scanners from the registry
     * 2. Creates ScanJob records in QUEUED status
     * 3. Enqueues each scanner invocation to JobRunr for parallel execution
     * 4. Transitions the ExtensionScan to SCANNING status
     * 5. Returns the scan ID for tracking
     * 
     * The scan ID is the ExtensionScan.id  which links
     * the high-level scan record with individual ScanJob records.
     * 
     * IMPORTANT: ScanJob records are created BEFORE JobRunr jobs are enqueued.
     * This avoids a race condition where AsyncScanCompletionService checks for
     * scan jobs before the JobRunr handler has created them.
     * 
     * JobRunr handles parallel execution, automatic retry, and persistence.
     * AsyncScanCompletionService will activate extensions when all scans complete.
     */
    public boolean submitScannerJobs(@NonNull ExtensionScan scan, @NonNull ExtensionVersion extVersion) {
        if (!config.isEnabled()) {
            logger.debug("Scanning is disabled, skipping scanner jobs for: {}", 
                NamingUtil.toLogFormat(extVersion));
            return false;
        }
        
        // Get all registered scanners
        List<Scanner> scanners = scannerRegistry.getAllScanners();
        
        if (scanners.isEmpty()) {
            logger.warn("No scanners registered, skipping scanner jobs for: {}", 
                NamingUtil.toLogFormat(extVersion));
            return false;
        }
        
        // Use ExtensionScan.id as the scan ID to link everything together
        String scanId = String.valueOf(scan.getId());
        long extensionVersionId = extVersion.getId();
        
        logger.debug("Submitting {} scanner jobs for extension: {} (scanId={})",
            scanners.size(), NamingUtil.toLogFormat(extVersion), scanId);
        
        // Transition to SCANNING status before submitting jobs
        transitionTo(scan, ScanStatus.SCANNING);
        
        // Create ScanJob records and enqueue JobRunr jobs
        int enqueuedCount = 0;
        for (Scanner scannerDef : scanners) {
            try {
                String scannerType = scannerDef.getScannerType();
                
                // Create ScanJob record FIRST (before enqueuing to JobRunr)
                // This ensures AsyncScanCompletionService can find the job records
                // even if it runs before the JobRunr handler executes
                ScannerJob job = new ScannerJob();
                job.setScanId(scanId);
                job.setScannerType(scannerType);
                job.setExtensionVersionId(extensionVersionId);
                job.setStatus(ScannerJob.JobStatus.QUEUED);
                job.setCreatedAt(LocalDateTime.now());
                job.setUpdatedAt(LocalDateTime.now());
                job.setPollLeaseUntil(null);
                job.setPollAttempts(0);
                job.setRecoveryInProgress(false);
                scanJobRepository.save(job);
                
                logger.debug("Created ScanJob record: {} for {} (scanId={})", 
                    scannerType, NamingUtil.toLogFormat(extVersion), scanId);
                
                // Now enqueue to JobRunr - the handler will find the existing ScanJob
                ScannerInvocationRequest jobRequest = new ScannerInvocationRequest(
                    scannerType,
                    extensionVersionId,
                    scanId
                );
                
                jobScheduler.enqueue(jobRequest);
                enqueuedCount++;
                
                logger.debug("Enqueued scanner job: {} for {} (scanId={})", 
                    scannerType, NamingUtil.toLogFormat(extVersion), scanId);
                
            } catch (Exception e) {
                logger.error("Failed to enqueue scanner {} for scanId={}", 
                    scannerDef.getScannerType(), scanId, e);
                // Continue with other scanners even if one fails to enqueue
            }
        }
        
        logger.debug("Enqueued {} of {} scanner jobs for: {} (scanId={})",
            enqueuedCount, scanners.size(), NamingUtil.toLogFormat(extVersion), scanId);
        
        return enqueuedCount > 0;
    }

    /**
     * Check if there are any scanners registered for long-running scans.
     */
    public boolean hasRegisteredScanners() {
        return !scannerRegistry.getAllScanners().isEmpty();
    }

    public void markScanPassed(@Nullable ExtensionScan scan) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping success marking",
                scan.getId(), scan.getStatus());
            return;
        }
        transitionToTerminal(scan, ScanStatus.PASSED);
    }

    public void quarantineScan(@Nullable ExtensionScan scan) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping quarantine marking",
                scan.getId(), scan.getStatus());
            return;
        }
        transitionToTerminal(scan, ScanStatus.QUARANTINED);
    }

    /**
     * Mark scan as REJECTED due to publish check/validation failure.
     * Used when policy violations or malicious content is detected during validation.
     */
    public void rejectScan(@Nullable ExtensionScan scan) {
        if (scan == null) return;
        if (scan.getStatus().isCompleted()) {
            logger.debug("Scan {} already completed with status {}, skipping rejection",
                scan.getId(), scan.getStatus());
            return;
        }
        transitionToTerminal(scan, ScanStatus.REJECTED);
    }

    public void markScanAsErrored(@Nullable ExtensionScan scan, @Nullable String errorMessage) {
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
            // VALIDATING can go to PASSED if no scanners, REJECTED for policy failures,
            // or QUARANTINED for security concerns
            case VALIDATING -> to == ScanStatus.SCANNING || to == ScanStatus.REJECTED || to == ScanStatus.PASSED || to == ScanStatus.QUARANTINED || to == ScanStatus.ERRORED;
            case SCANNING -> to == ScanStatus.PASSED || to == ScanStatus.QUARANTINED || to == ScanStatus.ERRORED;
            default -> false;
        };
    }

    public void removeScan(@NonNull ExtensionScan scan) {
        persistenceService.removeScan(scan);
    }

    /**
     * Build user-facing error message from findings.
     * 
     * Uses the userFacingMessage from each check, which may be masked for security
     * Detailed reasons are stored in the database for admin review.
     */
    private String getUserFacingErrorMessage(List<PublishCheckRunner.Finding> findings) {
        if (findings.isEmpty()) {
            return "Extension publication blocked.";
        }

        // Group findings by check type, using the user-facing message (one per check type).
        var messagesByType = findings.stream()
                .collect(Collectors.groupingBy(PublishCheckRunner.Finding::checkType));

        var parts = new java.util.ArrayList<String>();
        for (var entry : messagesByType.entrySet()) {
            var typeFindings = entry.getValue();
            String userMessage = typeFindings.getFirst().userFacingMessage();
            if (userMessage != null && !userMessage.isBlank()) {
                parts.add(userMessage);
            }
        }

        if (parts.isEmpty()) {
            return "Extension publication blocked.";
        }

        return "Extension publication blocked: " + String.join(", ", parts) + ". For details on " + 
            "publishing extensions, see: https://github.com/eclipse/openvsx/wiki/Publishing-Extensions";
    }
}