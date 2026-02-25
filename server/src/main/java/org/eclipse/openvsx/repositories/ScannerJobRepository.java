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
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.ScannerJob;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ScanJob entities.
 * 
 * Provides database access for scan job operations.
 * Used by the scanning service and polling service.
 */
public interface ScannerJobRepository extends Repository<ScannerJob, Long> {
    
    /**
     * Save a scan job.
     */
    ScannerJob save(ScannerJob scanJob);
    
    /**
     * Find a scan job by ID.
     */
    Optional<ScannerJob> findById(Long id);
    
    /**
     * Find all scan jobs for a specific scan ID.
     * 
     * Used to check if all jobs for an extension scan are complete.
     * 
     * @param scanId The scan ID that groups related jobs
     * @return List of all jobs with the given scan ID
     */
    List<ScannerJob> findByScanId(String scanId);
    
    /**
     * Find scan jobs by status.
     * 
     * Used by recovery monitor to find async jobs.
     */
    List<ScannerJob> findByStatusIn(List<ScannerJob.JobStatus> statuses);
    
    /**
     * Find scan job by external job ID.
     * 
     * Used to look up async jobs by their external scanner job ID.
     */
    Optional<ScannerJob> findByExternalJobId(String externalJobId);
    
    /**
     * Find all scan jobs for a specific extension version.
     * 
     * @param extensionVersionId The extension version ID
     * @return List of all jobs for the extension version
     */
    List<ScannerJob> findByExtensionVersionId(Long extensionVersionId);
    
    /**
     * Find a specific scan job by scan ID and scanner type.
     * 
     * This is used to avoid creating duplicate jobs during JobRunr retries.
     * When a JobRunr job retries, we want to update the same ScanJob record
     * rather than creating a new one.
     */
    Optional<ScannerJob> findByScanIdAndScannerType(String scanId, String scannerType);
    
    /**
     * Find scan jobs by status that were created before a certain time.
     * 
     * Used by the recovery service to find jobs stuck in QUEUED status.
     */
    List<ScannerJob> findByStatusAndCreatedAtBefore(ScannerJob.JobStatus status, LocalDateTime threshold);
    
    /**
     * Find scan jobs by status that were created before a certain time
     * and are not currently being recovered.
     * 
     * Used by recovery monitor to find stuck QUEUED jobs.
     * The recoveryInProgress flag prevents duplicate recovery attempts
     * when multiple pods are running.
     */
    List<ScannerJob> findByStatusAndCreatedAtBeforeAndRecoveryInProgressFalse(
        ScannerJob.JobStatus status, 
        LocalDateTime threshold
    );
    
    /**
     * Delete all scan jobs for a specific extension version.
     * 
     * Used when deleting an extension to clean up orphaned scan jobs.
     */
    void deleteByExtensionVersionId(Long extensionVersionId);
    
    /**
     * Delete all scan jobs for a specific scan ID.
     */
    void deleteByScanId(String scanId);
}

