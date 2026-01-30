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
     * <p>
     * This method creates the ScanJob record BEFORE invoking the scanner
     * to avoid creating duplicate records when JobRunr retries.
     * <p>
     * IMPORTANT: This method is NOT transactional as a whole because scanner.startScan()
     * makes HTTP calls to external services that can take minutes. Holding a database
     * connection during HTTP calls causes connection pool exhaustion.
     * <p>
     * Flow:
     * 1. Find or create ScanJob record (unique by scanId + scannerType) - status: QUEUED
     * 2. Get scanner from registry
     * 3. Mark job as PROCESSING (in transaction)
     * 4. Invoke scanner.startScan() (OUTSIDE transaction - may take minutes)
     * 5. Update job based on result (in transaction):
     *    - Sync scanner: COMPLETE (with results)
     *    - Async scanner: SUBMITTED (with external job ID for polling)
     * <p>
     * On retry (if step 4 fails):
     * - Same ScanJob is found and updated
     * - No duplicate records created
     * - Status transitions tracked via updatedAt
     * <p>
     * For sync scanners:
     * - startScan() returns immediate results
     * - Job marked COMPLETE
     * - Threats saved via ExtensionScanPersistenceService
     * <p>
     * For async scanners:
     * - startScan() returns external job ID
     * - Job marked SUBMITTED
     * - Polling service will check status periodically
     * <p>
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
    public void run(ScannerInvocationRequest jobRequest) throws Exception {
        String scannerType = jobRequest.getScannerType();
        long extensionVersionId = jobRequest.getExtensionVersionId();
        String scanId = jobRequest.getScanId();
        
        logger.debug("Invoking scanner: {} for extension version {}", 
            scannerType, extensionVersionId);
        
        // Phase 1: Prepare job (find-or-create + mark PROCESSING)
        // Returns null if job already terminal, or the scanner + job to process
        var prepared = prepareJob(scanId, scannerType, extensionVersionId);
        if (prepared == null) {
            return;  // Job already terminal, skip
        }
        
        Scanner scanner = prepared.scanner();
        Long jobId = prepared.jobId();
        
        try {
            // Phase 2: Invoke scanner (OUTSIDE transaction - may take minutes)
            // This is the slow part - HTTP calls to external services
            var command = new Scanner.Command(extensionVersionId, scanId);
            Scanner.Invocation invocation = scanner.startScan(command);
            
            // Phase 3: Save results
            // Note: processCompletedScan() has REQUIRES_NEW, so threats save in separate tx.
            // The job status update is just a single entity save (auto-commits).
            saveResults(jobId, invocation, scanner, scannerType, extensionVersionId);
            
        } catch (Exception e) {
            // Mark job as failed (single entity update, auto-commits)
            markJobFailed(jobId, scannerType, extensionVersionId, e);
            throw e;  // Re-throw to let JobRunr retry
        }
    }
    
    /**
     * Prepare the scanner job for invocation.
     * Creates or finds the job, validates it's not terminal, marks as PROCESSING.
     * Single entity operations - each save() auto-commits.
     */
    private PreparedJob prepareJob(String scanId, String scannerType, long extensionVersionId) {
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
            return null;
        }
        
        // 2. Get the scanner from registry
        Scanner scanner = scannerRegistry.getScanner(scannerType);
        
        if (scanner == null) {
            job.setStatus(ScannerJob.JobStatus.FAILED);
            job.setErrorMessage("Scanner not found: " + scannerType);
            job.setUpdatedAt(LocalDateTime.now());
            scanJobRepository.save(job);
            logger.error("Scanner not found: {}", scannerType);
            return null;  // Don't retry - scanner will never exist
        }
        
        // 3. Mark job as processing before starting scan
        // Also clear the recoveryInProgress flag if this is a recovered job
        job.setStatus(ScannerJob.JobStatus.PROCESSING);
        job.setRecoveryInProgress(false);
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
        
        return new PreparedJob(scanner, job.getId());
    }
    
    /**
     * Save the scanner invocation results.
     */
    private void saveResults(Long jobId, Scanner.Invocation invocation, Scanner scanner, 
                             String scannerType, long extensionVersionId) {
        ScannerJob job = scanJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
        
        // Determine the scan ID before processing (needed for completion check)
        String scanId = job.getScanId();
        
        // Update job status based on invocation result
        // For sync scanners: status -> COMPLETE, save threats
        // For async scanners: status -> SUBMITTED, store external job ID
        switch (invocation) {
            case Scanner.Invocation.Completed c -> 
                handleCompletedScan(job, c, scanner, scannerType, extensionVersionId);
            case Scanner.Invocation.Submitted s -> 
                handleSubmittedScan(job, s, scanner, scannerType, extensionVersionId);
        }
        
        scanJobRepository.save(job);
        
        // Now check if all jobs for this scan are complete.
        // Only do this for sync scanners (Completed) - async scanners will trigger
        // completion check when polling completes.
        if (invocation instanceof Scanner.Invocation.Completed) {
            completionService.checkCompletionSafely(scanId);
        }
    }
    
    /**
     * Mark a job as failed after an exception.
     */
    private void markJobFailed(Long jobId, String scannerType, long extensionVersionId, Exception e) {
        ScannerJob job = scanJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            logger.error("Cannot mark job {} as failed - not found", jobId);
            return;
        }
        
        job.setStatus(ScannerJob.JobStatus.FAILED);
        job.setErrorMessage("Scanner invocation failed: " + e.getMessage());
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
        
        logger.error("Failed to invoke scanner {} for extension version {}: {}", 
            scannerType, extensionVersionId, e.getMessage());
    }
    
    /**
     * Data holder for prepared job information.
     */
    public record PreparedJob(Scanner scanner, Long jobId) {}
    
    /**
     * Handle a completed (synchronous) scan result.
     * Marks job as complete, saves any threats found, and records result for audit.
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
