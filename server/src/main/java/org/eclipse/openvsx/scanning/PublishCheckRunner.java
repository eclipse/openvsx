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
import org.eclipse.openvsx.entities.ScanCheckResult;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs all publish checks against extension files and aggregates results.
 */
@Component
public class PublishCheckRunner{

    private static final Logger logger = LoggerFactory.getLogger(PublishCheckRunner.class);

    private final List<PublishCheck> checks;

    public PublishCheckRunner(List<PublishCheck> checks) {
        this.checks = checks;
        
        logger.info("PublishCheckRunner initialized with {} checks: {}",
            checks.size(),
            checks.stream().map(PublishCheck::getCheckType).toList());
    }
    
    /**
     * Get the list of check types that this runner will execute.
     */
    public List<String> getExpectedCheckTypes() {
        return checks.stream()
            .map(PublishCheck::getCheckType)
            .toList();
    }
    
    /**
     * Get the number of checks that will be executed.
     */
    public int getCheckCount() {
        return checks.size();
    }

    /**
     * Run all publish checks and return the aggregated result.
     * <p>
     * This method does not persist anything. It only executes checks and returns findings.
     * The caller is responsible for recording failures and managing state.
     */
    @NonNull
    public Result runChecks(
            @NonNull ExtensionScan scan,
            @NonNull TempFile extensionFile,
            @NonNull UserData user
    ) {
        var context = new PublishCheck.Context(scan, extensionFile, user);
        var allFindings = new ArrayList<Finding>();
        var checkExecutions = new ArrayList<CheckExecution>();
        boolean hasEnforcedFailure = false;
        Exception checkError = null;
        String errorCheckType = null;

        String extId = scan.getNamespaceName() + "." + scan.getExtensionName() + " " + scan.getExtensionVersion();
        
        for (var check : checks) {
            // Skip disabled checks - no record created
            if (!check.isEnabled()) {
                logger.debug("Scan {} ({}) - Skipping disabled check: {}", scan.getId(), extId, check.getCheckType());
                continue;
            }

            logger.debug("Scan {} ({}) - Running check: {}", scan.getId(), extId, check.getCheckType());
            
            var startTime = LocalDateTime.now();
            try {
                var result = check.check(context);
                var endTime = LocalDateTime.now();
                logger.debug("Scan {} ({}) - Check {} passed: {}", scan.getId(), extId, check.getCheckType(), result.passed());
                
                int findingsCount = 0;
                ScanCheckResult.CheckResult checkResult;
                String summary;
                
                if (result.passed()) {
                    checkResult = ScanCheckResult.CheckResult.PASSED;
                    summary = "No issues found";
                } else {
                    boolean enforced = check.isEnforced();
                    findingsCount = result.failures().size();
                    
                    // Get the user-facing message for this check's failures.
                    String userFacingMessage = check.getUserFacingMessage(result.failures());
                    
                    // Convert each failure to a Finding.
                    for (var failure : result.failures()) {
                        allFindings.add(new Finding(
                            check.getCheckType(),
                            failure.ruleName(),
                            failure.reason(),
                            enforced,
                            userFacingMessage
                        ));
                    }
                    
                    if (enforced) {
                        hasEnforcedFailure = true;
                        // Enforced issues reject the extension immediately
                        checkResult = ScanCheckResult.CheckResult.REJECT;
                        summary = String.format("Found %d issue(s) - rejected", findingsCount);
                    } else {
                        // Issues found but not enforced - extension still passes
                        checkResult = ScanCheckResult.CheckResult.PASSED;
                        summary = String.format("Found %d issue(s) - not enforced", findingsCount);
                    }
                    
                    logger.debug("Scan {} - {} detected {} issue(s), enforced={}",
                        scan.getId(), check.getCheckType(), findingsCount, enforced);
                }
                
                checkExecutions.add(new CheckExecution(
                    check.getCheckType(),
                    startTime,
                    endTime,
                    checkResult,
                    findingsCount,
                    null,
                    summary
                ));
                
            } catch (Exception e) {
                var endTime = LocalDateTime.now();
                // Capture the error but don't throw yet.
                // Let the caller decide how to handle it.
                checkError = e;
                errorCheckType = check.getCheckType();
                logger.warn("Scan {} ({}.{}.{}) - {} check threw exception: {}",
                    scan.getId(), scan.getNamespaceName(), scan.getExtensionName(),
                    scan.getExtensionVersion(), check.getCheckType(), e.getMessage());
                
                checkExecutions.add(new CheckExecution(
                    check.getCheckType(),
                    startTime,
                    endTime,
                    ScanCheckResult.CheckResult.ERROR,
                    0,
                    e.getMessage(),
                    "Error: " + e.getMessage()
                ));
                break;
            }
        }

        return new Result(allFindings, checkExecutions, hasEnforcedFailure, checkError, errorCheckType);
    }


    /**
     * Result of running all publish checks.
     */
    public record Result(
        @NonNull List<Finding> findings,
        @NonNull List<CheckExecution> checkExecutions,
        boolean hasEnforcedFailure,
        @Nullable Exception error,
        @Nullable String errorCheckType
    ) {
        /**
         * Check if all checks passed (no enforced failures and no errors).
         */
        public boolean passed() {
            return !hasEnforcedFailure && error == null;
        }

        /**
         * Check if there was an error during checks.
         */
        public boolean hasError() {
            return error != null;
        }

        /**
         * Get the error message if there was an error.
         */
        @Nullable
        public String getErrorMessage() {
            if (error == null) {
                return null;
            }
            return error.getMessage() + " For details, see: https://github.com/eclipse/openvsx/wiki/Publishing-Extensions";
        }

        /**
         * Get findings that are enforced (would block publication).
         */
        @NonNull
        public List<Finding> getEnforcedFindings() {
            return findings.stream()
                .filter(Finding::enforced)
                .toList();
        }

        /**
         * Get findings that are not enforced (warnings only).
         */
        @NonNull
        public List<Finding> getWarningFindings() {
            return findings.stream()
                .filter(f -> !f.enforced())
                .toList();
        }
    }

    /**
     * A single finding from a publish check.
     * Contains all info needed for the service to record the failure.
     */
    public record Finding(
        @NonNull String checkType,
        @NonNull String ruleName,
        @Nullable String reason,
        boolean enforced,
        @Nullable String userFacingMessage
    ) {}

    /**
     * Execution details for a single check.
     * Used to record all checks that were run for audit trail.
     */
    public record CheckExecution(
        @NonNull String checkType,
        @NonNull LocalDateTime startedAt,
        @NonNull LocalDateTime completedAt,
        @NonNull ScanCheckResult.CheckResult result,
        int findingsCount,
        @Nullable String errorMessage,
        @Nullable String summary
    ) {}
}
