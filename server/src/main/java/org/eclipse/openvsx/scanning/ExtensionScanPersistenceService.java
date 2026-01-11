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

import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static jakarta.transaction.Transactional.TxType;

/**
 * Handles all transactional persistence operations for extension scans.
 * This service is separate from ExtensionScanService to avoid self-invocation issues
 * where @Transactional(TxType.REQUIRES_NEW) would be ignored.
 * 
 * All methods use REQUIRES_NEW to ensure they commit independently,
 * preserving the scan audit trail even when the outer transaction rolls back.
 */
@Service
public class ExtensionScanPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionScanPersistenceService.class);

    private final RepositoryService repositories;

    public ExtensionScanPersistenceService(
            RepositoryService repositories
    ) {
        this.repositories = repositories;
    }

    /**
     * Creates and persists a new scan record BEFORE an extension version exists.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public ExtensionScan initializeScan(
            String namespaceName,
            String extensionName,
            String version,
            String targetPlatform,
            String displayName,
            UserData user
    ) {
        var isUniversal = targetPlatform == null || "universal".equals(targetPlatform);
        if (displayName == null || displayName.isBlank()) {
            displayName = extensionName;
        }
        
        return initializeScanInternal(
            namespaceName,
            extensionName,
            version,
            targetPlatform,
            isUniversal,
            displayName,
            user
        );
    }

    /**
     * Internal method that handles the actual scan creation logic.
     */
    private ExtensionScan initializeScanInternal(
            String namespaceName,
            String extensionName,
            String version,
            String targetPlatform,
            boolean isUniversal,
            String displayName,
            UserData user
    ) {
        try {
            var scan = new ExtensionScan();
            // Store raw values instead of foreign keys for audit trail
            scan.setNamespaceName(namespaceName);
            scan.setExtensionName(extensionName);
            scan.setExtensionVersion(version);
            scan.setTargetPlatform(targetPlatform);
            scan.setUniversalTargetPlatform(isUniversal);
            scan.setExtensionDisplayName(displayName);
            
            var publisherLoginName = "";
            String publisherUrl = null;
            if (user != null) {
                publisherLoginName = user.getLoginName() != null ? user.getLoginName() : "unknown";
                publisherUrl = user.getProviderUrl();
            }
            scan.setPublisher(publisherLoginName);
            scan.setPublisherUrl(publisherUrl);
            
            scan.setStartedAt(LocalDateTime.now());
            scan.setStatus(ScanStatus.STARTED);
            
            return repositories.saveExtensionScan(scan);
        } catch (Exception e) {
            logger.error("FATAL: Failed to create extension scan", e);
            throw e;
        }
    }

    /**
     * Records a validation failure with the given check type.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void recordValidationFailure(ExtensionScan scan, String checkType, String ruleName, String reason, boolean enforced) {
        var failure = ExtensionValidationFailure.create(checkType, ruleName, reason);
        failure.setEnforced(enforced);
        scan.addValidationFailure(failure);
        repositories.saveValidationFailure(failure);
    }

    /**
     * Records a threat detected by a security scanner during long-running scans.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void recordThreat(
            ExtensionScan scan,
            String fileName,
            String fileHash,
            String fileExtension,
            String scannerType,
            String ruleName,
            String reason,
            String severity,
            boolean enforced
    ) {
        var threat = ExtensionThreat.create(
                fileName,
                fileHash,
                fileExtension,
                scannerType,
                ruleName,
                reason,
                severity
        );
        threat.setEnforced(enforced);
        scan.addThreat(threat);
        repositories.saveExtensionThreat(threat);
    }

    /**
     * Persists a status change. The caller is responsible for validating the transition.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void updateStatus(ExtensionScan scan, ScanStatus newStatus) {
        scan.setStatus(newStatus);
        repositories.saveExtensionScan(scan);
    }

    /**
     * Persists a terminal status change with completion timestamp.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void completeWithStatus(ExtensionScan scan, ScanStatus newStatus) {
        scan.setStatus(newStatus);
        scan.setCompletedAt(LocalDateTime.now());
        repositories.saveExtensionScan(scan);
    }

    /**
     * Persists an error status with message.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void markAsErrored(ExtensionScan scan, String errorMessage) {
        scan.setStatus(ScanStatus.ERRORED);
        scan.setErrorMessage(errorMessage);
        scan.setCompletedAt(LocalDateTime.now());
        repositories.saveExtensionScan(scan);
    }

    /**
     * Removes a scan.
     */
    @Transactional(TxType.REQUIRES_NEW)
    public void removeScan(ExtensionScan scan) {
        repositories.deleteExtensionScan(scan);
    }
}

