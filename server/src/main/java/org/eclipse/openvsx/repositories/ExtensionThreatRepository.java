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
import org.eclipse.openvsx.entities.ExtensionThreat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for extension threat detection results.
 */
public interface ExtensionThreatRepository extends Repository<ExtensionThreat, Long> {

    /** Save a new or update an existing threat */
    ExtensionThreat save(ExtensionThreat threat);

    /** Find a threat by its ID */
    ExtensionThreat findById(long id);

    /** Find all threats for a specific scan */
    Streamable<ExtensionThreat> findByScan(ExtensionScan scan);

    /** Find all threats for a scan by scan ID */
    @Query("SELECT t FROM ExtensionThreat t WHERE t.scan.id = :scanId")
    Streamable<ExtensionThreat> findByScanId(long scanId);

    /** Find all threats detected by a specific scanner type */
    Streamable<ExtensionThreat> findByType(String type);

    /** Find all threats with a specific file hash */
    Streamable<ExtensionThreat> findByFileHash(String fileHash);

    /** Count all threats for a scan */
    long countByScan(ExtensionScan scan);

    /** Check if any threats exist for a scan */
    boolean existsByScan(ExtensionScan scan);

    /** Check if threats from a specific scanner type exist for a scan */
    boolean existsByScanAndType(ExtensionScan scan, String type);

    /** Find all threats for a scan with a specific scanner type */
    Streamable<ExtensionThreat> findByScanAndType(ExtensionScan scan, String type);

    /** Find all threats detected after a specific date */
    Streamable<ExtensionThreat> findByDetectedAtAfter(LocalDateTime date);

    /** Count all threats by scanner type */
    long countByType(String type);

    /** Find all threats for a scan, ordered by detection time */
    Streamable<ExtensionThreat> findByScanOrderByDetectedAtAsc(ExtensionScan scan);

    /** Find distinct scanner types from all threats (for filter options) */
    @Query("SELECT DISTINCT t.type FROM ExtensionThreat t ORDER BY t.type")
    List<String> findDistinctScannerTypes();

    /** Find distinct rule names from all threats (for filter options) */
    @Query("SELECT DISTINCT t.ruleName FROM ExtensionThreat t ORDER BY t.ruleName")
    List<String> findDistinctRuleNames();

    /** Delete a threat by ID */
    void deleteById(long id);

    /** Delete all threats for a scan */
    void deleteByScan(ExtensionScan scan);
}

