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

import jakarta.annotation.PostConstruct;
import org.eclipse.openvsx.entities.ScanStatus;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Recovers scans that were left in non-terminal states after a server restart.
 * Runs once on application startup.
 */
@Component
public class StaleScanRecovery {

    private static final Logger logger = LoggerFactory.getLogger(StaleScanRecovery.class);

    private final RepositoryService repositories;
    private final ExtensionScanPersistenceService persistenceService;

    public StaleScanRecovery(
            RepositoryService repositories,
            ExtensionScanPersistenceService persistenceService
    ) {
        this.repositories = repositories;
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    public void recoverStaleScans() {

        Arrays.stream(ScanStatus.values())
            .filter(status -> !status.isCompleted())
            .forEach(status -> {
                var staleScans = repositories.findExtensionScansByStatus(status).toList();
                int recoveredCount = 0;

                for (var scan : staleScans) {
                    var message = String.format(
                        "Scan interrupted by server restart (was %s)", 
                        status
                    );
                    
                    try {
                        persistenceService.markAsErrored(scan, message);
                        recoveredCount++;
                    } catch (Exception e) {
                        logger.error("Failed to recover stale scan {}: {}", 
                            scan.getId(), e.getMessage());
                    }
                }

                if (recoveredCount > 0) {
                    logger.info("Recovered {} stale scan(s) on startup", recoveredCount);
                }
            });

    }
}

