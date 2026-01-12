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
import org.eclipse.openvsx.entities.ExtensionValidationFailure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing ExtensionValidationFailure entities.
 */
public interface ExtensionValidationFailureRepository extends Repository<ExtensionValidationFailure, Long> {

    /** Save a new or update an existing validation failure */
    ExtensionValidationFailure save(ExtensionValidationFailure failure);

    /** Find a validation failure by its ID */
    ExtensionValidationFailure findById(long id);

    /** Find all validation failures for a specific scan */
    Streamable<ExtensionValidationFailure> findByScan(ExtensionScan scan);

    /** Find all validation failures of a specific check type */
    Streamable<ExtensionValidationFailure> findByCheckType(String checkType);

    /** Find all validation failures for a scan with a specific check type */
    Streamable<ExtensionValidationFailure> findByScanAndCheckType(ExtensionScan scan, String checkType);

    /** Find all validation failures detected after a specific date */
    Streamable<ExtensionValidationFailure> findByDetectedAtAfter(LocalDateTime date);

    /** Count all validation failures for a specific scan */
    long countByScan(ExtensionScan scan);

    /** Count all validation failures of a specific check type */
    long countByCheckType(String checkType);

    /** Check if any validation failures exist for a scan */
    boolean existsByScan(ExtensionScan scan);

    /** Check if validation failures of a specific type exist for a scan */
    boolean existsByScanAndCheckType(ExtensionScan scan, String checkType);

    /** Delete all validation failures for a scan */
    void deleteByScan(ExtensionScan scan);

    /** Delete a validation failure by ID */
    void deleteById(long id);

    /** Find all validation failures for a scan, ordered by detection time */
    Streamable<ExtensionValidationFailure> findByScanOrderByDetectedAtAsc(ExtensionScan scan);

    /**
     * Returns a sorted list of distinct rule names.
     */
    @Query("select distinct f.ruleName from ExtensionValidationFailure f order by f.ruleName")
    List<String> findDistinctRuleNames();

    /**
     * Returns a sorted list of distinct check types.
     */
    @Query("select distinct f.checkType from ExtensionValidationFailure f order by f.checkType")
    List<String> findDistinctCheckTypes();
}

