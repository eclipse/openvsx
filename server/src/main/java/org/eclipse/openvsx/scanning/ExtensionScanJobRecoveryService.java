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

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.ExtensionScan;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.ScanCheckResult;
import org.eclipse.openvsx.entities.ScannerJob;
import org.eclipse.openvsx.entities.ScanStatus;
import org.eclipse.openvsx.publish.PublishExtensionVersionService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.repositories.ScannerJobRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles scan job recovery: startup recovery and runtime watchdog.
 */
@Service
public class ExtensionScanJobRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionScanJobRecoveryService.class);

    // Max jobs to process per watchdog cycle
    private static final int MAX_PER_CYCLE = 20;

    private final ScannerJobRepository scanJobRepository;
    private final ScannerRegistry scannerRegistry;
    private final RepositoryService repositories;
    private final ExtensionScanPersistenceService persistenceService;
    private final ExtensionScanService scanService;
    private final ExtensionScanCompletionService completionService;
    private final PublishCheckRunner publishCheckRunner;
    private final PublishExtensionVersionService publishService;
    private final ExtensionService extensionService;
    private final JobRequestScheduler jobScheduler;

    public ExtensionScanJobRecoveryService(
            ScannerJobRepository scanJobRepository,
            ScannerRegistry scannerRegistry,
            RepositoryService repositories,
            ExtensionScanPersistenceService persistenceService,
            ExtensionScanService scanService,
            ExtensionScanCompletionService completionService,
            PublishCheckRunner publishCheckRunner,
            PublishExtensionVersionService publishService,
            ExtensionService extensionService,
            JobRequestScheduler jobScheduler
    ) {
        this.scanJobRepository = scanJobRepository;
        this.scannerRegistry = scannerRegistry;
        this.repositories = repositories;
        this.persistenceService = persistenceService;
        this.scanService = scanService;
        this.completionService = completionService;
        this.publishCheckRunner = publishCheckRunner;
        this.publishService = publishService;
        this.extensionService = extensionService;
        this.jobScheduler = jobScheduler;
    }

    /**
     * Recover all scan state on server startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverOnStartup() {
        logger.info("Starting scan recovery on server startup");
        
        recoverPendingJobs();
        recoverValidatingScans();
        recoverStaleScans();
    }

    /**
     * Handle jobs pending when server restarted.
     * - Scanner removed: Mark job as REMOVED
     * - Async job (SUBMITTED/PROCESSING): Schedule immediate poll to resume
     */
    private void recoverPendingJobs() {
        List<ScannerJob> pendingJobs = scanJobRepository.findByStatusIn(
            List.of(
                ScannerJob.JobStatus.QUEUED,
                ScannerJob.JobStatus.PROCESSING,
                ScannerJob.JobStatus.SUBMITTED
            )
        );

        if (pendingJobs.isEmpty()) {
            logger.debug("No pending scan jobs found");
            return;
        }

        logger.info("Found {} pending scan jobs, checking for orphaned jobs", pendingJobs.size());

        int orphanedCount = 0;
        int validJobCount = 0;
        int asyncJobsScheduled = 0;

        for (ScannerJob job : pendingJobs) {
            String scannerType = job.getScannerType();
            Scanner scanner = scannerRegistry.getScanner(scannerType);

            if (scanner == null) {
                // Scanner no longer exists - mark as REMOVED
                logger.warn("Found orphaned job for removed scanner: {} scanId={}", scannerType, job.getScanId());
                job.setStatus(ScannerJob.JobStatus.REMOVED);
                job.setErrorMessage("Scanner '" + scannerType + "' removed from configuration");
                job.setUpdatedAt(LocalDateTime.now());
                scanJobRepository.save(job);
                orphanedCount++;
            } else {
                validJobCount++;
                
                // Schedule recovery poll for async jobs
                if (scanner.isAsync() && 
                    (job.getStatus() == ScannerJob.JobStatus.SUBMITTED || 
                     job.getStatus() == ScannerJob.JobStatus.PROCESSING)) {
                    try {
                        Instant pollTime = Instant.now().plusSeconds(10);
                        jobScheduler.schedule(pollTime, new ScannerPollRequest(job.getId()));
                        asyncJobsScheduled++;
                        logger.info("Scheduled recovery poll for job {} (scanner: {})", job.getId(), scannerType);
                    } catch (Exception e) {
                        logger.warn("Failed to schedule recovery poll for job {}: {}", job.getId(), e.getMessage());
                    }
                }
            }
        }

        if (orphanedCount > 0) {
            logger.warn("Marked {} orphaned jobs as REMOVED, {} valid jobs remain", orphanedCount, validJobCount);
        }
        if (asyncJobsScheduled > 0) {
            logger.info("Scheduled {} recovery polls for async jobs", asyncJobsScheduled);
        }
    }

    /**
     * Recover scans stuck in VALIDATING status.
     */
    private void recoverValidatingScans() {
        var validatingScans = repositories.findExtensionScansByStatus(ScanStatus.VALIDATING).toList();
        
        if (validatingScans.isEmpty()) {
            logger.debug("No scans stuck in VALIDATING status");
            return;
        }
        
        logger.info("Found {} scan(s) stuck in VALIDATING", validatingScans.size());
        
        int recovered = 0, errored = 0, rejected = 0, skipped = 0;
        
        for (var scan : validatingScans) {
            try {
                var result = recoverValidatingScan(scan);
                switch (result) {
                    case RECOVERED -> recovered++;
                    case ERRORED -> errored++;
                    case REJECTED -> rejected++;
                    case SKIPPED -> skipped++;
                }
            } catch (Exception e) {
                logger.error("Failed to recover VALIDATING scan {} ({}.{}.{}): {}", 
                    scan.getId(), scan.getNamespaceName(), scan.getExtensionName(), scan.getExtensionVersion(), e.getMessage());
                try {
                    persistenceService.markAsErrored(scan, "Recovery failed: " + e.getMessage());
                    errored++;
                } catch (Exception ex) {
                    logger.error("Failed to mark scan {} ({}.{}.{}) as ERRORED", 
                        scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                        scan.getExtensionVersion());
                }
            }
        }
        
        logger.info("VALIDATING recovery: {} recovered, {} rejected, {} errored, {} skipped",
            recovered, rejected, errored, skipped);
    }
    
    private enum RecoveryResult { RECOVERED, ERRORED, REJECTED, SKIPPED }
    
    private RecoveryResult recoverValidatingScan(ExtensionScan scan) {
        var checkResults = repositories.findScanCheckResultsByScanId(scan.getId());
        var publishCheckResults = checkResults.stream()
            .filter(r -> r.getCategory() == ScanCheckResult.CheckCategory.PUBLISH_CHECK)
            .toList();
        
        if (publishCheckResults.isEmpty()) {
            logger.warn("Scan {} ({}.{}.{}) stuck in VALIDATING with no check results", 
                scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                scan.getExtensionVersion());
            persistenceService.markAsErrored(scan, "No publish check results found");
            return RecoveryResult.ERRORED;
        }
        
        // Verify all expected checks completed
        var expectedCheckTypes = publishCheckRunner.getExpectedCheckTypes();
        var recordedCheckTypes = publishCheckResults.stream()
            .map(ScanCheckResult::getCheckType)
            .toList();
        var missingChecks = expectedCheckTypes.stream()
            .filter(type -> !recordedCheckTypes.contains(type))
            .toList();
        
        if (!missingChecks.isEmpty()) {
            logger.info("Scan {} has {} of {} checks complete, skipping (may still be running)",
                scan.getId(), recordedCheckTypes.size(), expectedCheckTypes.size());
            return RecoveryResult.SKIPPED;
        }
        
        // Check outcomes
        boolean hasRejection = publishCheckResults.stream()
            .anyMatch(r -> r.getResult() == ScanCheckResult.CheckResult.REJECT);
        if (hasRejection) {
            logger.warn("Scan {} ({}.{}.{}) with REJECT result, completing as REJECTED", 
                scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                scan.getExtensionVersion());
            scan.setStatus(ScanStatus.REJECTED);
            scan.setCompletedAt(LocalDateTime.now());
            repositories.saveExtensionScan(scan);
            return RecoveryResult.REJECTED;
        }
        
        boolean hasError = publishCheckResults.stream()
            .anyMatch(r -> r.getResult() == ScanCheckResult.CheckResult.ERROR);
        if (hasError) {
            persistenceService.markAsErrored(scan, "Publish check had error");
            return RecoveryResult.ERRORED;
        }
        
        // All checks passed - resume scan
        logger.info("Scan {} ({}.{}.{}) has all {} checks passed, resuming", 
            scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
            scan.getExtensionVersion(), publishCheckResults.size());
        
        var extVersion = findExtensionVersion(scan);
        if (extVersion == null) {
            persistenceService.markAsErrored(scan, "Extension version no longer exists");
            return RecoveryResult.ERRORED;
        }
        
        try {
            boolean submitted = scanService.submitScannerJobs(scan, extVersion);
            if (!submitted) {
                // No scanners configured — safe to activate
                logger.info("Scan {} ({}.{}.{}) has no scanners, activating extension", 
                    scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                    scan.getExtensionVersion());
                publishService.activateExtension(extVersion, extensionService);
                scanService.markScanPassed(scan);
            }
            return RecoveryResult.RECOVERED;
        } catch (Exception e) {
            // Enqueue attempts failed — do NOT activate. Mark as errored.
            logger.error("Scan {} ({}.{}.{}) failed to submit scanner jobs during recovery",
                scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                scan.getExtensionVersion(), e);
            persistenceService.markAsErrored(scan, "Recovery failed to submit scanner jobs: " + e.getMessage());
            return RecoveryResult.ERRORED;
        }
    }
    
    private ExtensionVersion findExtensionVersion(ExtensionScan scan) {
        try {
            var extension = repositories.findExtension(scan.getExtensionName(), scan.getNamespaceName());
            if (extension == null) return null;
            return repositories.findVersion(scan.getExtensionVersion(), scan.getTargetPlatform(), extension);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Handle scans stuck in incomplete status with no active jobs.
     * This can happen if:
     * - Server restarted while scans were in progress
     * - Completion check failed to transition scan after all jobs finished
     * - Jobs failed to enqueue properly
     */
    private void recoverStaleScans() {
        int erroredCount = 0;
        int recoverableCount = 0;
        int completedCount = 0;

        for (ScanStatus status : ScanStatus.values()) {
            if (status.isCompleted() || status == ScanStatus.VALIDATING) {
                continue;
            }

            var staleScans = repositories.findExtensionScansByStatus(status).toList();

            for (var scan : staleScans) {
                String scanIdStr = String.valueOf(scan.getId());
                List<ScannerJob> jobs = scanJobRepository.findByScanId(scanIdStr);
                boolean hasRecoverableJobs = jobs.stream().anyMatch(job -> 
                    job.getStatus() == ScannerJob.JobStatus.QUEUED ||
                    job.getStatus() == ScannerJob.JobStatus.PROCESSING ||
                    job.getStatus() == ScannerJob.JobStatus.SUBMITTED
                );

                if (hasRecoverableJobs) {
                    logger.info("Scan {} ({}.{}.{}) has recoverable jobs, leaving in {} status", 
                        scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                        scan.getExtensionVersion(), status);
                    recoverableCount++;
                    continue;
                }

                // Check if all jobs are complete (no failures)
                boolean allJobsComplete = !jobs.isEmpty() && jobs.stream()
                    .allMatch(j -> j.getStatus() == ScannerJob.JobStatus.COMPLETE);
                
                if (allJobsComplete) {
                    // All jobs complete - retry completion check
                    logger.info("Retrying completion for scan {} ({}.{}.{}) - all {} jobs complete",
                        scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                        scan.getExtensionVersion(), jobs.size());
                    try {
                        completionService.checkSingleScanCompletion(scanIdStr);
                        completedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to complete scan {} ({}.{}.{}): {}",
                            scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                            scan.getExtensionVersion(), e.getMessage());
                        persistenceService.markAsErrored(scan, "Completion retry failed: " + e.getMessage());
                        erroredCount++;
                    }
                    continue;
                }

                // Determine reason based on job state
                String reason;
                if (jobs.isEmpty()) {
                    reason = String.format("Scan stuck in %s with no scanner jobs created", status);
                } else {
                    long completedJobs = jobs.stream()
                        .filter(j -> j.getStatus() == ScannerJob.JobStatus.COMPLETE)
                        .count();
                    long failedJobs = jobs.stream()
                        .filter(j -> j.getStatus() == ScannerJob.JobStatus.FAILED)
                        .count();
                    reason = String.format("Scan stuck in %s with %d complete, %d failed jobs", 
                        status, completedJobs, failedJobs);
                }

                try {
                    logger.warn("Marking stale scan {} ({}.{}.{}) as ERRORED: {}", 
                        scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                        scan.getExtensionVersion(), reason);
                    persistenceService.markAsErrored(scan, reason);
                    erroredCount++;
                } catch (Exception e) {
                    logger.error("Failed to recover stale scan {} ({}.{}.{})", 
                        scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                        scan.getExtensionVersion());
                }
            }
        }

        if (recoverableCount > 0) logger.info("Left {} scan(s) for recovery", recoverableCount);
        if (completedCount > 0) logger.info("Completed {} scan(s) via recovery", completedCount);
        if (erroredCount > 0) logger.info("Marked {} scan(s) as ERRORED", erroredCount);
    }

    /**
     * Monitor stuck jobs and timeouts.
     */
    @Job(name = "Scan job watchdog", retries = 0)
    @Recurring(id = "scan-job-watchdog", interval = "PT10M")
    @Transactional
    public void runWatchdog() {
        recoverStuckQueuedJobs();
        checkTimeouts();
    }
    
    /**
     * Recover jobs stuck in QUEUED status.
     * Re-enqueue if stuck 5-60 min, fail if stuck > 60 min.
     */
    private void recoverStuckQueuedJobs() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusMinutes(60);
        
        List<ScannerJob> stuckJobs = scanJobRepository.findByStatusAndCreatedAtBeforeAndRecoveryInProgressFalse(
            ScannerJob.JobStatus.QUEUED, fiveMinutesAgo);
        
        if (stuckJobs.isEmpty()) return;
        
        logger.warn("Found {} jobs stuck in QUEUED", stuckJobs.size());
        
        int processed = 0;
        for (ScannerJob job : stuckJobs) {
            if (processed >= MAX_PER_CYCLE) break;
            processed++;
            
            if (job.getCreatedAt().isBefore(oneHourAgo)) {
                markFailed(job, "Stuck in QUEUED for over 1 hour");
                completionService.checkCompletionSafely(job.getScanId());
            } else {
                try {
                    job.setRecoveryInProgress(true);
                    job.setUpdatedAt(LocalDateTime.now());
                    scanJobRepository.save(job);
                    
                    jobScheduler.enqueue(new ScannerInvocationRequest(
                        job.getScannerType(), job.getExtensionVersionId(), job.getScanId()));
                    logger.info("Re-enqueued stuck job: scanId={}, scanner={}", job.getScanId(), job.getScannerType());
                } catch (Exception e) {
                    logger.error("Failed to re-enqueue job {}", job.getScanId());
                    job.setRecoveryInProgress(false);
                    markFailed(job, "Recovery failed: " + e.getMessage());
                    completionService.checkCompletionSafely(job.getScanId());
                }
            }
        }
    }
    
    /**
     * Check for jobs exceeding scanner timeout.
     */
    private void checkTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        
        List<ScannerJob> asyncJobs = scanJobRepository.findByStatusIn(
            List.of(ScannerJob.JobStatus.SUBMITTED, ScannerJob.JobStatus.PROCESSING));
        
        int timedOut = 0;
        for (ScannerJob job : asyncJobs) {
            Scanner scanner = scannerRegistry.getScanner(job.getScannerType());
            if (scanner == null) continue;
            
            int timeoutMinutes = scanner.getTimeoutMinutes();
            long ageMinutes = Duration.between(job.getCreatedAt(), now).toMinutes();
            
            if (ageMinutes >= timeoutMinutes) {
                markFailed(job, String.format("Timeout: exceeded %d min limit (age: %d min)", timeoutMinutes, ageMinutes));
                completionService.checkCompletionSafely(job.getScanId());
                timedOut++;
                logger.error("Job {} timed out: scanner={}, age={}min", job.getId(), job.getScannerType(), ageMinutes);
            }
        }
        
        if (timedOut > 0) logger.info("Timed out {} jobs", timedOut);
    }
    
    private void markFailed(ScannerJob job, String errorMessage) {
        job.setStatus(ScannerJob.JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setPollLeaseUntil(null);
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
    }
}
