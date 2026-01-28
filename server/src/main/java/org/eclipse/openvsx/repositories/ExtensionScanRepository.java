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
import org.eclipse.openvsx.entities.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import javax.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository for accessing ExtensionScan entities.
 */
public interface ExtensionScanRepository extends Repository<ExtensionScan, Long> {

    /** Save a new or update an existing scan */
    ExtensionScan save(ExtensionScan scan);

    /** Find a scan by its ID */
    ExtensionScan findById(long id);

    /** Find all scans for a specific extension version */
    Streamable<ExtensionScan> findByNamespaceNameAndExtensionNameAndExtensionVersionAndTargetPlatform(
        String namespaceName, String extensionName, String extensionVersion, String targetPlatform);

    /** Find the most recent scan for a specific extension version */
    ExtensionScan findFirstByNamespaceNameAndExtensionNameAndExtensionVersionAndTargetPlatformOrderByStartedAtDesc(
        String namespaceName, String extensionName, String extensionVersion, String targetPlatform);

    /** Find all scans for a specific extension */
    Streamable<ExtensionScan> findByNamespaceNameAndExtensionName(String namespaceName, String extensionName);

    /** Find all scans for a namespace */
    Streamable<ExtensionScan> findByNamespaceName(String namespaceName);

    /** Find all scans with a specific status */
    Streamable<ExtensionScan> findByStatus(ScanStatus status);
    
    /** Find the oldest N scans with a specific status (for batch processing) */
    @Query(value = """
        SELECT s.* FROM extension_scan s
        WHERE s.status = :status
        ORDER BY s.started_at ASC NULLS LAST
        LIMIT :limit
        """, nativeQuery = true)
    List<ExtensionScan> findOldestByStatus(@Param("status") String status, @Param("limit") int limit);
    
    /** Find oldest N scans with a specific status (convenience overload) */
    default List<ExtensionScan> findOldestByStatus(ScanStatus status, int limit) {
        return findOldestByStatus(status.name(), limit);
    }

    /** Find all scans that are still in progress */
    Streamable<ExtensionScan> findByCompletedAtIsNull();

    /** Find all scans started after a specific date */
    Streamable<ExtensionScan> findByStartedAtAfter(LocalDateTime date);

    /** Find all scans with specific status started after a date */
    Streamable<ExtensionScan> findByStatusAndStartedAtAfter(ScanStatus status, LocalDateTime date);

    /** Count all scans with a specific status */
    long countByStatus(ScanStatus scanning);

    /** Count scans for a specific extension */
    long countByNamespaceNameAndExtensionName(String namespaceName, String extensionName);

    /** Delete a scan by ID */
    void deleteById(long id);

    /** Find all scans by status, ordered by start date */
    Streamable<ExtensionScan> findByStatusOrderByStartedAtDesc(ScanStatus status);

    /** Check if a scan exists for a specific version with a given status */
    boolean existsByNamespaceNameAndExtensionNameAndExtensionVersionAndTargetPlatformAndStatus(
        String namespaceName, String extensionName, String extensionVersion, String targetPlatform, ScanStatus status);

    /** Find all scans ordered by start date */
    Streamable<ExtensionScan> findAllByOrderByStartedAtDesc();

    /**
     * Paginated query with optional filters for status, namespace, publisher, name, and date range.
     * 
     * Filter parameters:
     * - statuses: list of ScanStatus values (empty = no filter)
     * - namespace: partial match on namespace_name (null/empty = no filter)
     * - publisher: partial match on publisher (null/empty = no filter)  
     * - name: partial match on extension_name OR extension_display_name (null/empty = no filter)
     * - startedFrom/startedTo: date range filter on started_at (null = no filter)
     */
    @Query(value = """
        SELECT s.* FROM extension_scan s
        WHERE (CAST(:statuses AS TEXT) IS NULL OR s.status IN (:statuses))
          AND (CAST(:namespace AS TEXT) IS NULL OR LOWER(s.namespace_name) LIKE LOWER('%' || :namespace || '%'))
          AND (CAST(:publisher AS TEXT) IS NULL OR LOWER(s.publisher) LIKE LOWER('%' || :publisher || '%'))
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(s.extension_name) LIKE LOWER('%' || :name || '%')
               OR LOWER(s.extension_display_name) LIKE LOWER('%' || :name || '%'))
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
        """, nativeQuery = true)
    Page<ExtensionScan> findScansFiltered(
        @Param("statuses") Collection<String> statuses,
        @Param("namespace") String namespace,
        @Param("publisher") String publisher,
        @Param("name") String name,
        @Param("startedFrom") LocalDateTime startedFrom,
        @Param("startedTo") LocalDateTime startedTo,
        Pageable pageable
    );

    /**
     * Count scans matching filters (for totalSize without loading all records).
     */
    @Query(value = """
        SELECT COUNT(*) FROM extension_scan s
        WHERE (CAST(:statuses AS TEXT) IS NULL OR s.status IN (:statuses))
          AND (CAST(:namespace AS TEXT) IS NULL OR LOWER(s.namespace_name) LIKE LOWER('%' || :namespace || '%'))
          AND (CAST(:publisher AS TEXT) IS NULL OR LOWER(s.publisher) LIKE LOWER('%' || :publisher || '%'))
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(s.extension_name) LIKE LOWER('%' || :name || '%')
               OR LOWER(s.extension_display_name) LIKE LOWER('%' || :name || '%'))
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
        """, nativeQuery = true)
    long countScansFiltered(
        @Param("statuses") Collection<String> statuses,
        @Param("namespace") String namespace,
        @Param("publisher") String publisher,
        @Param("name") String name,
        @Param("startedFrom") LocalDateTime startedFrom,
        @Param("startedTo") LocalDateTime startedTo
    );

    /**
     * Count scans by status with date range filter.
     */
    @Query(value = """
        SELECT COUNT(*) FROM extension_scan
        WHERE status = :#{#status.name()}
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR started_at <= :startedTo)
        """, nativeQuery = true)
    long countByStatusAndDateRange(
        @Param("status") ScanStatus status,
        @Param("startedFrom") LocalDateTime startedFrom,
        @Param("startedTo") LocalDateTime startedTo
    );

    /**
     * Count scans by status with date range AND enforcement filter.
     */
    @Query(value = """
        SELECT COUNT(*) FROM extension_scan s
        WHERE s.status = :#{#status.name()}
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
          AND EXISTS (
              SELECT 1 FROM extension_validation_failure f
              WHERE f.scan_id = s.id AND f.enforced = :enforcedOnly)
        """, nativeQuery = true)
    long countByStatusDateRangeAndEnforcement(
        @Param("status") ScanStatus status,
        @Param("startedFrom") LocalDateTime startedFrom,
        @Param("startedTo") LocalDateTime startedTo,
        @Param("enforcedOnly") boolean enforcedOnly
    );

    /**
     * Count scans for statistics with all filters.
     * 
     * Enforcement behavior matches the list endpoint:
     * - When checkTypes is specified: enforcement modifies that filter (AND logic)
     * - When scannerNames is specified: enforcement modifies that filter (AND logic)
     * - When neither is specified: enforcement filters on ANY validation failure or threat
     */
    @Query(value = """
        SELECT COUNT(*) FROM extension_scan s
        WHERE s.status = :status
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
          AND (:applyCheckTypesFilter = false OR EXISTS (
               SELECT 1 FROM extension_validation_failure f
               WHERE f.scan_id = s.id
                 AND f.validation_type IN (:checkTypes)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR f.enforced = :enforcedOnly)))
          AND (:applyScannerNamesFilter = false OR EXISTS (
               SELECT 1 FROM extension_threat t
               WHERE t.scan_id = s.id
                 AND t.scanner_type IN (:scannerNames)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR t.enforced = :enforcedOnly)))
          AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL
               OR :applyCheckTypesFilter = true
               OR :applyScannerNamesFilter = true
               OR EXISTS (SELECT 1 FROM extension_validation_failure f WHERE f.scan_id = s.id AND f.enforced = :enforcedOnly)
               OR EXISTS (SELECT 1 FROM extension_threat t WHERE t.scan_id = s.id AND t.enforced = :enforcedOnly))
        """, nativeQuery = true)
    long countForStatistics(
        @Param("status") String status,
        @Nullable @Param("startedFrom") LocalDateTime startedFrom,
        @Nullable @Param("startedTo") LocalDateTime startedTo,
        @Param("checkTypes") Collection<String> checkTypes,
        @Param("applyCheckTypesFilter") boolean applyCheckTypesFilter,
        @Param("scannerNames") Collection<String> scannerNames,
        @Param("applyScannerNamesFilter") boolean applyScannerNamesFilter,
        @Nullable @Param("enforcedOnly") Boolean enforcedOnly
    );

    /**
     * Full paginated query with ALL filters including validationType, scannerNames, enforcement, and adminDecision.
     * 
     * Enforcement behavior:
     * - When validationType is specified: enforcement modifies that filter (AND logic)
     * - When threatScannerName is specified: enforcement modifies that filter (AND logic)
     * - When neither is specified: enforcement filters on ANY validation failure or threat
     */
    @Query(value = """
        SELECT s.* FROM extension_scan s
        WHERE (CAST(:statuses AS TEXT) IS NULL OR s.status IN (:statuses))
          AND (CAST(:namespace AS TEXT) IS NULL OR LOWER(s.namespace_name) LIKE LOWER('%' || :namespace || '%'))
          AND (CAST(:publisher AS TEXT) IS NULL OR LOWER(s.publisher) LIKE LOWER('%' || :publisher || '%'))
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(s.extension_name) LIKE LOWER('%' || :name || '%')
               OR LOWER(s.extension_display_name) LIKE LOWER('%' || :name || '%'))
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
          AND (:applyCheckTypesFilter = false OR EXISTS (
               SELECT 1 FROM extension_validation_failure f
               WHERE f.scan_id = s.id
                 AND f.validation_type IN (:checkTypes)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR f.enforced = :enforcedOnly)))
          AND (:applyScannerNamesFilter = false OR EXISTS (
               SELECT 1 FROM extension_threat t
               WHERE t.scan_id = s.id
                 AND t.scanner_type IN (:scannerNames)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR t.enforced = :enforcedOnly)))
          AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL
               OR :applyCheckTypesFilter = true
               OR :applyScannerNamesFilter = true
               OR EXISTS (SELECT 1 FROM extension_validation_failure f WHERE f.scan_id = s.id AND f.enforced = :enforcedOnly)
               OR EXISTS (SELECT 1 FROM extension_threat t WHERE t.scan_id = s.id AND t.enforced = :enforcedOnly))
          AND (:applyAdminDecisionFilter = false OR (
               (:filterAllowed = true AND EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id AND d.decision = 'ALLOWED'))
               OR (:filterBlocked = true AND EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id AND d.decision = 'BLOCKED'))
               OR (:filterNeedsReview = true AND s.status = 'QUARANTINED' AND NOT EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id))))
        """, nativeQuery = true)
    Page<ExtensionScan> findScansFullyFiltered(
        @Nullable @Param("statuses") Collection<String> statuses,
        @Nullable @Param("namespace") String namespace,
        @Nullable @Param("publisher") String publisher,
        @Nullable @Param("name") String name,
        @Nullable @Param("startedFrom") LocalDateTime startedFrom,
        @Nullable @Param("startedTo") LocalDateTime startedTo,
        @Param("checkTypes") Collection<String> checkTypes,
        @Param("applyCheckTypesFilter") boolean applyCheckTypesFilter,
        @Param("scannerNames") Collection<String> scannerNames,
        @Param("applyScannerNamesFilter") boolean applyScannerNamesFilter,
        @Nullable @Param("enforcedOnly") Boolean enforcedOnly,
        @Param("applyAdminDecisionFilter") boolean applyAdminDecisionFilter,
        @Param("filterAllowed") boolean filterAllowed,
        @Param("filterBlocked") boolean filterBlocked,
        @Param("filterNeedsReview") boolean filterNeedsReview,
        Pageable pageable
    );

    /**
     * Count version of findScansFullyFiltered.
     */
    @Query(value = """
        SELECT COUNT(*) FROM extension_scan s
        WHERE (CAST(:statuses AS TEXT) IS NULL OR s.status IN (:statuses))
          AND (CAST(:namespace AS TEXT) IS NULL OR LOWER(s.namespace_name) LIKE LOWER('%' || :namespace || '%'))
          AND (CAST(:publisher AS TEXT) IS NULL OR LOWER(s.publisher) LIKE LOWER('%' || :publisher || '%'))
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(s.extension_name) LIKE LOWER('%' || :name || '%')
               OR LOWER(s.extension_display_name) LIKE LOWER('%' || :name || '%'))
          AND (CAST(:startedFrom AS TIMESTAMP) IS NULL OR s.started_at >= :startedFrom)
          AND (CAST(:startedTo AS TIMESTAMP) IS NULL OR s.started_at <= :startedTo)
          AND (:applyCheckTypesFilter = false OR EXISTS (
               SELECT 1 FROM extension_validation_failure f
               WHERE f.scan_id = s.id
                 AND f.validation_type IN (:checkTypes)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR f.enforced = :enforcedOnly)))
          AND (:applyScannerNamesFilter = false OR EXISTS (
               SELECT 1 FROM extension_threat t
               WHERE t.scan_id = s.id
                 AND t.scanner_type IN (:scannerNames)
                 AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL OR t.enforced = :enforcedOnly)))
          AND (CAST(:enforcedOnly AS BOOLEAN) IS NULL
               OR :applyCheckTypesFilter = true
               OR :applyScannerNamesFilter = true
               OR EXISTS (SELECT 1 FROM extension_validation_failure f WHERE f.scan_id = s.id AND f.enforced = :enforcedOnly)
               OR EXISTS (SELECT 1 FROM extension_threat t WHERE t.scan_id = s.id AND t.enforced = :enforcedOnly))
          AND (:applyAdminDecisionFilter = false OR (
               (:filterAllowed = true AND EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id AND d.decision = 'ALLOWED'))
               OR (:filterBlocked = true AND EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id AND d.decision = 'BLOCKED'))
               OR (:filterNeedsReview = true AND s.status = 'QUARANTINED' AND NOT EXISTS (SELECT 1 FROM admin_scan_decision d WHERE d.scan_id = s.id))))
        """, nativeQuery = true)
    long countScansFullyFiltered(
        @Nullable @Param("statuses") Collection<String> statuses,
        @Nullable @Param("namespace") String namespace,
        @Nullable @Param("publisher") String publisher,
        @Nullable @Param("name") String name,
        @Nullable @Param("startedFrom") LocalDateTime startedFrom,
        @Nullable @Param("startedTo") LocalDateTime startedTo,
        @Param("checkTypes") Collection<String> checkTypes,
        @Param("applyCheckTypesFilter") boolean applyCheckTypesFilter,
        @Param("scannerNames") Collection<String> scannerNames,
        @Param("applyScannerNamesFilter") boolean applyScannerNamesFilter,
        @Nullable @Param("enforcedOnly") Boolean enforcedOnly,
        @Param("applyAdminDecisionFilter") boolean applyAdminDecisionFilter,
        @Param("filterAllowed") boolean filterAllowed,
        @Param("filterBlocked") boolean filterBlocked,
        @Param("filterNeedsReview") boolean filterNeedsReview
    );
}

