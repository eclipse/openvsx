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

import org.eclipse.openvsx.entities.ExtensionScan;
import org.eclipse.openvsx.entities.ScanCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ScanCheckResult entities.
 * Provides query methods for retrieving check execution history.
 */
@Repository
public interface ScanCheckResultRepository extends JpaRepository<ScanCheckResult, Long> {

    /**
     * Find all check results for a scan, ordered by start time.
     */
    List<ScanCheckResult> findByScanOrderByStartedAtAsc(ExtensionScan scan);

    /**
     * Find all check results for a scan ID, ordered by start time.
     */
    List<ScanCheckResult> findByScanIdOrderByStartedAtAsc(long scanId);

    /**
     * Find check results by scan and category.
     */
    List<ScanCheckResult> findByScanAndCategoryOrderByStartedAtAsc(
            ExtensionScan scan, 
            ScanCheckResult.CheckCategory category
    );

    /**
     * Find check result by scan and check type.
     * Returns the most recent result if multiple exist.
     */
    ScanCheckResult findFirstByScanAndCheckTypeOrderByStartedAtDesc(
            ExtensionScan scan, 
            String checkType
    );

    /**
     * Check if a check result exists for a scan and check type.
     */
    boolean existsByScanIdAndCheckType(long scanId, String checkType);

    /**
     * Delete all check results for a scan.
     */
    void deleteByScan(ExtensionScan scan);
}
