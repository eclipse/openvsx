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

import org.eclipse.openvsx.entities.ExtensionScan;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the scanning logic for extensions.
 * 
 * This class is responsible for:
 * - Running validation checks against extension files
 * - Collecting and aggregating check results
 * 
 * It does NOT handle:
 * - Persistence or State transitions - that's the responsibility of the {@link ExtensionScanService}
 */
@Component
public class ExtensionScanner {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionScanner.class);

    private final List<ValidationCheck> validationChecks;

    public ExtensionScanner(List<ValidationCheck> validationChecks) {
        this.validationChecks = validationChecks;
        
        logger.info("ExtensionScanner initialized with {} validation checks: {}",
            validationChecks.size(),
            validationChecks.stream().map(ValidationCheck::getCheckType).toList());
    }

    /**
     * Run all validation checks and return the aggregated result.
     * 
     * This method does not persist anything. It only executes checks and returns findings.
     * The caller is responsible for recording failures and managing state.
     */
    public ScanResult runValidationChecks(
            ExtensionScan scan,
            TempFile extensionFile,
            UserData user
    ) {
        var context = new ValidationCheck.Context(scan, extensionFile, user);
        var allFindings = new ArrayList<CheckFinding>();
        boolean hasEnforcedFailure = false;
        Exception checkError = null;
        String errorCheckType = null;

        for (var check : validationChecks) {
            // Skip disabled checks.
            if (!check.isEnabled()) {
                logger.debug("Scan {} - Skipping disabled check: {}", scan.getId(), check.getCheckType());
                continue;
            }

            logger.debug("Scan {} - Running check: {}", scan.getId(), check.getCheckType());
            
            try {
                var result = check.check(context);
                logger.debug("Scan {} - Check {} passed: {}", scan.getId(), check.getCheckType(), result.passed());
                
                if (!result.passed()) {
                    boolean enforced = check.isEnforced();
                    
                    // Convert each failure to a CheckFinding.
                    for (var failure : result.failures()) {
                        allFindings.add(new CheckFinding(
                            check.getCheckType(),
                            failure.ruleName(),
                            failure.reason(),
                            enforced
                        ));
                    }
                    
                    if (enforced) {
                        hasEnforcedFailure = true;
                    }
                    
                    logger.debug("Scan {} - {} detected {} issue(s), enforced={}",
                        scan.getId(), check.getCheckType(), result.failures().size(), enforced);
                }
            } catch (Exception e) {
                // Capture the error but don't throw yet.
                // Let the caller decide how to handle it.
                checkError = e;
                errorCheckType = check.getCheckType();
                logger.warn("Scan {} - {} check threw exception: {}",
                    scan.getId(), check.getCheckType(), e.getMessage());
                break;
            }
        }

        return new ScanResult(allFindings, hasEnforcedFailure, checkError, errorCheckType);
    }

    public void runScanners(ExtensionScan scan, TempFile extensionFile, UserData user) {
        // TODO: Implement scan checks.
    }

    /**
     * Result of running all validation checks.
     */
    public record ScanResult(
        List<CheckFinding> findings,
        boolean hasEnforcedFailure,
        Exception error,
        String errorCheckType
    ) {
        /**
         * Check if all validation passed (no enforced failures and no errors).
         */
        public boolean passed() {
            return !hasEnforcedFailure && error == null;
        }

        /**
         * Check if there was an error during validation.
         */
        public boolean hasError() {
            return error != null;
        }

        /**
         * Get the error message if there was an error.
         */
        public String getErrorMessage() {
            if (error == null) {
                return null;
            }
            return error.getMessage() + " For details, see: https://github.com/eclipse/openvsx/wiki/Publishing-Extensions";
        }

        /**
         * Get findings that are enforced (would block publication).
         */
        public List<CheckFinding> getEnforcedFindings() {
            return findings.stream()
                .filter(CheckFinding::enforced)
                .toList();
        }

        /**
         * Get findings that are not enforced (warnings only).
         */
        public List<CheckFinding> getWarningFindings() {
            return findings.stream()
                .filter(f -> !f.enforced())
                .toList();
        }
    }

    /**
     * A single finding from a validation check.
     * Contains all info needed for the service to record the failure.
     */
    public record CheckFinding(
        String checkType,
        String ruleName,
        String reason,
        boolean enforced
    ) {}
}
