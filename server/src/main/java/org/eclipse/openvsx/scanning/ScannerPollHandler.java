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

/**
 * JobRunr handler that polls async scan jobs.
 * Self-schedules with exponential backoff until complete/failed.
 */
@Component
public class ScannerPollHandler implements JobRequestHandler<ScannerPollRequest> {
    
    protected final Logger logger = LoggerFactory.getLogger(ScannerPollHandler.class);
    
    private final ScannerJobRepository scanJobRepository;
    private final ScannerRegistry scannerRegistry;
    private final ExtensionScanPersistenceService persistenceService;
    private final JobRequestScheduler jobScheduler;
    private final ExtensionScanCompletionService completionService;
    
    public ScannerPollHandler(
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
     * Poll a single scan job.
     * 
     * This method:
     * 1. Loads the scan job from database
     * 2. Gets the appropriate scanner using the scanner type
     * 3. Polls the scanner for status
     * 4. Updates the database based on the result
     * 
     * JobRunr automatically:
     * - Runs this in a worker thread
     * - Retries on failure (up to 3 times)
     * - Tracks job status in database
     * 
     * @param jobRequest The request containing the scan job ID to poll
     */
    @Override
    @Job(
        name = "Poll async scan job", 
        retries = 3    // Retry 3 times on failure
    )
    @Transactional
    public void run(ScannerPollRequest jobRequest) throws Exception {
        long scanJobId = jobRequest.getScanJobId();
        
        // Load the scan job from database
        // Job may have been deleted if extension was deleted - silently skip
        var optionalJob = scanJobRepository.findById(scanJobId);
        if (optionalJob.isEmpty()) {
            logger.debug("Scan job {} no longer exists (extension may have been deleted), skipping poll", scanJobId);
            return;
        }
        
        ScannerJob job = optionalJob.get();
        
        // Skip if job is already in terminal state
        // This can happen if another poll completed the job before this one ran
        if (job.getStatus().isTerminal()) {
            logger.debug("Scan job {} already in terminal state {}, skipping poll", 
                scanJobId, job.getStatus());
            return;
        }
        
        logger.debug("Polling scan job {} (scanner: {}, external ID: {})", 
            scanJobId, job.getScannerType(), job.getExternalJobId());
        
        try {
            // Get the scanner for this job type
            // Use scanner type to identify which scanner to invoke
            Scanner scanner = scannerRegistry.getScanner(job.getScannerType());
            
            if (scanner == null) {
                logger.error("Scanner not found: {}", job.getScannerType());
                return;
            }
            
            if (!scanner.isAsync()) {
                // Sync scanner shouldn't be in pending state
                // This indicates a bug in the scanning service
                logger.warn("Sync scanner found in pending jobs: {}", job.getScannerType());
                job.setPollLeaseUntil(null);  // Clear lease
                scanJobRepository.save(job);
                return;
            }
            
            // Poll the scanner for status
            String externalJobId = job.getExternalJobId();
            if (externalJobId == null) {
                logger.error("Scan job {} has null external job ID, cannot poll", scanJobId);
                markJobFailed(job, "Missing external job ID");
                return;
            }
            var submission = new Scanner.Submission(externalJobId);
            Scanner.PollStatus status = scanner.pollStatus(submission);
            
            logger.debug("Scan job {} status: {}", scanJobId, status);
            
            // Handle the status
            handleScanStatus(job, scanner, submission, status);
            
        } catch (Exception e) {
            // Error during polling
            // JobRunr will retry this job automatically
            logger.error("Error polling scan job " + scanJobId, e);
            
            // Clear lease so job can be polled again
            job.setPollLeaseUntil(null);
            scanJobRepository.save(job);
            throw e;  // Let JobRunr handle retry
        }
    }
    
    /**
     * Handle the scan status response.
     * 
     * Updates the scan job in the database based on the status.
     * If the scan is complete, retrieves the results.
     */
    private void handleScanStatus(
        ScannerJob job,
        Scanner scanner,
        Scanner.Submission submission,
        Scanner.PollStatus status
    ) throws ScannerException {
        switch (status) {
            case COMPLETED -> handleCompletedStatus(job, scanner, submission);
            case FAILED -> handleFailedStatus(job);
            case PROCESSING, SUBMITTED -> handleProcessingStatus(job, scanner);
        }
    }
    
    /**
     * Handle COMPLETED status: retrieve results, save threats, record result for audit, and trigger completion check.
     */
    private void handleCompletedStatus(ScannerJob job, Scanner scanner, Scanner.Submission submission) 
            throws ScannerException {
        logger.debug("Scan job {} completed, retrieving results", job.getId());
        
        Scanner.Result result = scanner.fetchResults(submission);
        
        // Mark job complete and clear lease
        job.setStatus(ScannerJob.JobStatus.COMPLETE);
        job.setPollLeaseUntil(null);  // Clear lease - no more polling needed
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
        
        // Process result: save threats, determine check result, record audit
        var processed = persistenceService.processCompletedScan(
            job, result, scanner.enforcesThreats());
        
        // Log based on result - only log threats at INFO/WARN level
        if (processed.threatCount() == 0) {
            logger.debug("Scan job {} found no threats", job.getId());
        } else if (processed.checkResult() == ScanCheckResult.CheckResult.QUARANTINE) {
            logger.warn("Scan job {} found threats: {}", job.getId(), processed.summary());
        } else {
            logger.info("Scan job {} found issues: {}", job.getId(), processed.summary());
        }
        
        // Check completion AFTER transaction commits so other jobs are visible
        completionService.checkCompletionSafely(job.getScanId());
    }
    
    /**
     * Handle FAILED status: mark job as failed, record result for audit, and trigger completion check.
     */
    private void handleFailedStatus(ScannerJob job) {
        logger.error("Scan job {} failed at external scanner", job.getId());
        markJobFailed(job, "External scan failed");
        
        // Record scanner job result for audit trail
        persistenceService.recordScannerJobResult(
            job.getScanId(),
            job,
            ScanCheckResult.CheckResult.ERROR,
            null,  // filesScanned
            0,     // threatCount
            "External scan failed",
            "External scan failed"
        );
        
        // Still check completion - might need to mark scan as errored
        completionService.checkCompletionSafely(job.getScanId());
    }
    
    /**
     * Handle PROCESSING/SUBMITTED status: schedule next poll.
     * 
     * Uses scanner's poll config for interval, backoff, and max attempts.
     * Falls back to defaults if scanner has no poll config.
     */
    private void handleProcessingStatus(ScannerJob job, Scanner scanner) {
        // Get poll config from scanner, or use default if not specified
        var pollConfig = scanner.getPollConfig();
        if (pollConfig == null) {
            pollConfig = RemoteScannerProperties.PollConfig.DEFAULT;
        }
        
        // Increment poll attempts
        job.incrementPollAttempts();
        int attempts = job.getPollAttempts();
        
        // Check if we've exceeded max attempts
        int maxAttempts = pollConfig.getMaxAttempts();
        if (attempts >= maxAttempts) {
            logger.error("Scan job {} exceeded max poll attempts ({}), marking as failed", 
                job.getId(), maxAttempts);
            markJobFailed(job, "Exceeded max poll attempts (" + maxAttempts + ")");
            completionService.checkCompletionSafely(job.getScanId());
            return;
        }
        
        // Update job status
        job.setStatus(ScannerJob.JobStatus.PROCESSING);
        job.setPollLeaseUntil(null);  // No lease needed with self-scheduling
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
        
        // Calculate delay based on poll config
        int delaySeconds = calculatePollDelay(attempts, pollConfig);
        
        // Schedule next poll
        scheduleNextPoll(job.getId(), delaySeconds);
        
        logger.debug("Scan job {} still processing (attempt {}), next poll in {}s", 
            job.getId(), attempts, delaySeconds);
    }
    
    /**
     * Calculate poll delay based on scanner's poll configuration.
     * 
     * If exponential backoff is enabled:
     *   delay = min(interval * multiplier^(attempt-1), maxInterval)
     * Otherwise:
     *   delay = interval (fixed)
     */
    private int calculatePollDelay(int attempts, RemoteScannerProperties.PollConfig pollConfig) {
        int interval = pollConfig.getIntervalSeconds();
        int maxInterval = pollConfig.getMaxIntervalSeconds();
        boolean useBackoff = pollConfig.isExponentialBackoff();
        double multiplier = pollConfig.getBackoffMultiplier();
        
        if (!useBackoff) {
            // Fixed interval polling
            return interval;
        }
        
        // Exponential backoff: interval * multiplier^(attempt-1)
        // maxInterval caps the result, so just calculate and cap
        double delay = interval * Math.pow(multiplier, attempts - 1);
        return (int) Math.min(delay, maxInterval);
    }
    
    /**
     * Schedule the next poll using JobRunr's built-in delay.
     */
    private void scheduleNextPoll(Long jobId, int delaySeconds) {
        try {
            Instant pollTime = Instant.now().plusSeconds(delaySeconds);
            jobScheduler.schedule(pollTime, new ScannerPollRequest(jobId));
        } catch (Exception e) {
            // Log but don't fail - recovery monitor will catch orphaned jobs
            logger.warn("Failed to schedule next poll for job {}: {}", jobId, e.getMessage());
        }
    }
    
    /**
     * Mark a job as failed with error message.
     */
    private void markJobFailed(ScannerJob job, String errorMessage) {
        job.setStatus(ScannerJob.JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setPollLeaseUntil(null);  // Clear lease - no more polling needed
        job.setUpdatedAt(LocalDateTime.now());
        scanJobRepository.save(job);
    }
    
}
