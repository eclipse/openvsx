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

import org.eclipse.openvsx.entities.ScanCheckResult;
import org.eclipse.openvsx.entities.ScannerJob;
import org.eclipse.openvsx.repositories.ScannerJobRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * JobRunr handler that invokes a single scanner.
 * Runs in background worker threads with automatic retry.
 */
@Component
public class ScannerInvocationHandler implements JobRequestHandler<ScannerInvocationRequest> {
    
    protected final Logger logger = LoggerFactory.getLogger(ScannerInvocationHandler.class);
    
    private final ScannerJobRepository scanJobRepository;
    private final ScannerRegistry scannerRegistry;
    private final ExtensionScanPersistenceService persistenceService;
    private final JobRequestScheduler jobScheduler;
    private final ExtensionScanCompletionService completionService;
    
    public ScannerInvocationHandler(
            ScannerJobRepository scanJobRepository,
            ScannerRegistry scannerRegistry,
            ExtensionScanPersistenceService persistenceService,
            JobRequestScheduler jobScheduler,
            ExtensionScanCompletionService completionService
    ) {
        this.scanJobRepository = scanJobRepository;
        this.scannerRegistry = scannerRegistry;
        this.persistenceService = persistenceService;
        this.jobScheduler = jobScheduler;
        this.completionService = completionService;
    }
    
    
    /**
     * Invoke a single scanner and persist the result.
     * 
     * This method creates the ScanJob record BEFORE invoking the scanner
     * to avoid creating duplicate records when JobRunr retries.
     * 
     * Flow:
     * 1. Find or create ScanJob record (unique by scanId + scannerType) - status: QUEUED
     * 2. Get scanner from registry
     * 3. Mark job as PROCESSING
     * 4. Invoke scanner.startScan()
     * 5. Update job based on result:
     *    - Sync scanner: COMPLETE (with results)
     *    - Async scanner: SUBMITTED (with external job ID for polling)
     * 
     * On retry (if step 4 fails):
     * - Same ScanJob is found and updated
     * - No duplicate records created
     * - Status transitions tracked via updatedAt
     * 
     * For sync scanners:
     * - startScan() returns immediate results
     * - Job marked COMPLETE
     * - Threats saved via ExtensionScanPersistenceService
     * 
     * For async scanners:
     * - startScan() returns external job ID
     * - Job marked SUBMITTED
     * - Polling service will check status periodically
     * 
     * JobRunr automatically:
     * - Runs this in a worker thread
     * - Retries on failure (up to 2 times)
     * - Tracks job status in database
     */
    @Override
    @Job(
        name = "Invoke scanner for extension scan", 
        retries = 2    // Retry 2 times on failure
    )
    @Transactional
    public void run(ScannerInvocationRequest jobRequest) throws Exception {
        String scannerType = jobRequest.getScannerType();
        long extensionVersionId = jobRequest.getExtensionVersionId();
        String scanId = jobRequest.getScanId();
        
        logger.debug("Invoking scanner: {} for extension version {}", 
            scannerType, extensionVersionId);
        
        // 1. CREATE OR FIND EXISTING SCANJOB FIRST
        // This handles JobRunr retries - we update the same job, not create duplicates
        ScannerJob job = scanJobRepository
            .findByScanIdAndScannerType(scanId, scannerType)
            .orElseGet(() -> {
                ScannerJob newJob = new ScannerJob();
                newJob.setScanId(scanId);
                newJob.setScannerType(scannerType);
                newJob.setExtensionVersionId(extensionVersionId);
                newJob.setStatus(ScannerJob.JobStatus.QUEUED);  // Queued, waiting to invoke scanner
                newJob.setCreatedAt(LocalDateTime.now());
                newJob.setUpdatedAt(LocalDateTime.now());
                newJob.setPollLeaseUntil(null);  // No lease yet
                newJob.setPollAttempts(0);  // No poll attempts yet
                newJob.setRecoveryInProgress(false);
                return scanJobRepository.save(newJob);
            });
        
        // Skip if job is already in terminal state
        // This can happen if this is a duplicate enqueue or recovery attempt
        if (job.getStatus().isTerminal()) {
            logger.debug("Scan job for scanner {} already in terminal state {}, skipping invocation", 
                scannerType, job.getStatus());
            return;
        }
        
        try {
            // 2. Get the scanner from registry
            Scanner scanner = scannerRegistry.getScanner(scannerType);
            
            if (scanner == null) {
                job.setStatus(ScannerJob.JobStatus.FAILED);
                job.setErrorMessage("Scanner not found: " + scannerType);
                job.setUpdatedAt(LocalDateTime.now());
                scanJobRepository.save(job);
                logger.error("Scanner not found: {}", scannerType);
                return;  // Don't retry - scanner will never exist
            }
            
            // 3. Create scan command
            // Scanners will retrieve the file themselves using ScanFileService
            var command = new Scanner.Command(extensionVersionId, scanId);
            
            // 4. Mark job as processing before starting scan
            // Also clear the recoveryInProgress flag if this is a recovered job
            job.setStatus(ScannerJob.JobStatus.PROCESSING);
            job.setRecoveryInProgress(false);
            job.setUpdatedAt(LocalDateTime.now());
            scanJobRepository.save(job);
            
            // 5. Start the scan (this might fail and cause retry)
            Scanner.Invocation invocation = scanner.startScan(command);
            
            // 6. Update job based on scan result using pattern matching
            switch (invocation) {
                case Scanner.Invocation.Completed c -> 
                    handleCompletedScan(job, c, scanner, scannerType, extensionVersionId);
                case Scanner.Invocation.Submitted s -> 
                    handleSubmittedScan(job, s, scanner, scannerType, extensionVersionId);
            }
            
            // Save updated scan job to database
            scanJobRepository.save(job);
            
        } catch (Exception e) {
            // 7. Mark job as failed and let JobRunr retry
            job.setStatus(ScannerJob.JobStatus.FAILED);
            job.setErrorMessage("Scanner invocation failed: " + e.getMessage());
            job.setUpdatedAt(LocalDateTime.now());
            scanJobRepository.save(job);
            
            logger.error("Failed to invoke scanner {} for extension version {}: {}", 
                scannerType, extensionVersionId, e.getMessage());
            
            // Re-throw to let JobRunr retry
            // On retry, the same ScanJob will be found and updated
            throw e;
        }
    }
    
    /**
     * Handle a completed (synchronous) scan result.
     * Marks job as complete, saves any threats found, records result for audit, and triggers completion check.
     */
    private void handleCompletedScan(
            ScannerJob job, 
            Scanner.Invocation.Completed completed, 
            Scanner scanner,
            String scannerType, 
            long extensionVersionId
    ) {
        // Mark job as complete
        job.setStatus(ScannerJob.JobStatus.COMPLETE);
        job.setExternalJobId(null);  // No external job for sync scanners
        job.setUpdatedAt(LocalDateTime.now());
        
        // Process result: save threats, determine check result, record audit
        var processed = persistenceService.processCompletedScan(
            job, completed.result(), scanner.enforcesThreats());
        
        // Only log when threats were found
        if (processed.checkResult() == ScanCheckResult.CheckResult.QUARANTINE) {
            logger.warn("Scanner {} found threats: {} (extension version: {})",
                scannerType, processed.summary(), extensionVersionId);
        } else if (processed.threatCount() > 0) {
            // Threats found but not enforced (warnings only)
            logger.debug("Scanner {} found issues: {} (extension version: {})",
                scannerType, processed.summary(), extensionVersionId);
        }
        // Clean results (threatCount == 0) are not logged - too noisy
        
        // Check completion inline - sees this job's status (in-transaction) 
        // and other jobs' committed statuses. If not all jobs done yet,
        // the last job to finish will trigger completion.
        completionService.checkCompletionSafely(job.getScanId());
    }
    
    /**
     * Handle a submitted (asynchronous) scan.
     * Marks job as submitted, stores file hashes, and schedules first poll.
     */
    private void handleSubmittedScan(
            ScannerJob job, 
            Scanner.Invocation.Submitted submitted, 
            Scanner scanner,
            String scannerType, 
            long extensionVersionId
    ) {
        var submission = submitted.submission();
        String externalJobId = submission.externalJobId();
        
        // Mark job as submitted to external service (requires polling)
        job.setStatus(ScannerJob.JobStatus.SUBMITTED);
        job.setExternalJobId(externalJobId);
        job.setUpdatedAt(LocalDateTime.now());
        
        // Store file hashes for async scanners with file extraction
        // This allows looking up hashes when results come back later
        if (submission.hasFileHashes()) {
            Map<String, String> fileHashes = submission.fileHashes();
            String fileHashesJson = persistenceService.serializeFileHashes(fileHashes);
            if (fileHashesJson != null) {
                job.setFileHashesJson(fileHashesJson);
                logger.debug("Stored {} file hashes for async scan job {}", 
                    fileHashes.size(), externalJobId);
            }
        }
        
        logger.debug("Scanner {} submitted to external service. Job ID: {} (extension version: {})",
            scannerType, externalJobId, extensionVersionId);
        
        // Schedule first poll using scanner's poll configuration
        // No need for a separate polling monitor - polls chain themselves
        scheduleFirstPoll(job.getId(), scanner);
    }
    
    /**
     * Schedule the first poll for an async scan job.
     * Uses JobRunr's schedule() for delayed execution.
     * Subsequent polls are scheduled by PollScannerHandler with backoff.
     */
    private void scheduleFirstPoll(Long jobId, Scanner scanner) {
        try {
            // Get poll config from scanner, or use default if not specified
            var pollConfig = scanner.getPollConfig();
            if (pollConfig == null) {
                pollConfig = RemoteScannerProperties.PollConfig.DEFAULT;
            }
            int delaySeconds = pollConfig.getInitialDelaySeconds();
            
            Instant pollTime = Instant.now().plusSeconds(delaySeconds);
            jobScheduler.schedule(pollTime, new ScannerPollRequest(jobId));
            logger.debug("Scheduled first poll for job {} at {} (delay: {}s)", jobId, pollTime, delaySeconds);
        } catch (Exception e) {
            // Log but don't fail - recovery monitor will catch orphaned jobs
            logger.warn("Failed to schedule first poll for job {}: {}", jobId, e.getMessage());
        }
    }
}
