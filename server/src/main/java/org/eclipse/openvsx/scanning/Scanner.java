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

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base interface for all scanner implementations.
 * 
 * Each scanner implementation (internal checks, external scanners, etc.)
 * must implement this interface. Scanners can be either synchronous
 * (returning results immediately) or asynchronous (requiring polling).
 */
public interface Scanner {
    
    /**
     * Command to start a scan. Contains metadata about what to scan.
     * Scanners retrieve the actual file via extensionVersionId using ScannerFileService.
     */
    record Command(long extensionVersionId, @NonNull String scanId) {}
    
    /**
     * Represents a scan that has been submitted to an external service.
     */
    record Submission(@NonNull String externalJobId, @Nullable Map<String, String> fileHashes) {
        public Submission(@NonNull String externalJobId) {
            this(externalJobId, null);
        }
        
        @NonNull
        public Map<String, String> fileHashes() {
            return fileHashes != null ? fileHashes : Collections.emptyMap();
        }
        
        public boolean hasFileHashes() {
            return fileHashes != null && !fileHashes.isEmpty();
        }
    }
    
    /**
     * Status returned by {@link #pollStatus(Submission)}.
     */
    enum PollStatus {
        SUBMITTED,
        PROCESSING,
        COMPLETED,
        FAILED
    }
    
    /**
     * Result of a completed scan.
     */
    class Result {
        private final boolean clean;
        private final List<Threat> threats;
        
        private Result(boolean clean, List<Threat> threats) {
            this.clean = clean;
            this.threats = new ArrayList<>(threats);
        }
        
        @NonNull
        public static Result clean() {
            return new Result(true, Collections.emptyList());
        }
        
        @NonNull
        public static Result withThreats(@NonNull List<Threat> threats) {
            return new Result(false, threats);
        }
        
        public boolean isClean() {
            return clean;
        }
        
        @NonNull
        public List<Threat> getThreats() {
            return Collections.unmodifiableList(threats);
        }
    }
    
    /**
     * A security threat found during scanning.
     */
    class Threat {
        private final String name;
        private final String description;
        private final String severity;
        private final String filePath;
        private final String fileHash;
        
        public Threat(@NonNull String name, @Nullable String description, @NonNull String severity) {
            this(name, description, severity, null, null);
        }
        
        public Threat(@NonNull String name, @Nullable String description, @NonNull String severity, @Nullable String filePath) {
            this(name, description, severity, filePath, null);
        }
        
        public Threat(@NonNull String name, @Nullable String description, @NonNull String severity, @Nullable String filePath, @Nullable String fileHash) {
            this.name = name;
            this.description = description;
            this.severity = severity;
            this.filePath = filePath;
            this.fileHash = fileHash;
        }
        
        @NonNull public String getName() { return name; }
        @Nullable public String getDescription() { return description; }
        @NonNull public String getSeverity() { return severity; }
        @Nullable public String getFilePath() { return filePath; }
        @Nullable public String getFileHash() { return fileHash; }
    }
    
    /**
     * Result of invoking a scanner.
     * 
     * A scanner returns either:
     * - Completed: Sync scan with immediate results
     * - Submitted: Async scan that requires polling
     */
    sealed interface Invocation {
        record Completed(@NonNull Result result) implements Invocation {}
        record Submitted(@NonNull Submission submission) implements Invocation {}
    }
    
    /**
     * Returns the unique type identifier for this scanner.
     */
    @NonNull
    String getScannerType();
    
    /**
     * Indicates if this scanner is required for extension activation.
     * If true: Scanner failure blocks activation (fail-closed)
     * If false: Scanner failure is logged but extension can still activate
     */
    default boolean isRequired() {
        return true;
    }
    
    /**
     * Indicates if threats from this scanner should block extension activation.
     * If true: Threats quarantine the extension
     * If false: Threats are logged as warnings but extension can activate
     */
    default boolean enforcesThreats() {
        return true;
    }
    
    /**
     * Returns the timeout duration in minutes for async scanners.
     */
    default int getTimeoutMinutes() {
        return 60;
    }
    
    /**
     * Indicates if this scanner is asynchronous.
     */
    boolean isAsync();
    
    /**
     * Get the polling configuration for this async scanner.
     * Returns null to use defaults.
     */
    @Nullable
    default RemoteScannerProperties.PollConfig getPollConfig() {
        return null;
    }
    
    /**
     * Start a scan and return the invocation result.
     */
    @NonNull
    Invocation startScan(@NonNull Command command) throws ScannerException;
    
    /**
     * Poll status of an async scan job.
     */
    @NonNull
    default PollStatus pollStatus(@NonNull Submission submission) throws ScannerException {
        throw new UnsupportedOperationException(
            "Scanner " + getScannerType() + " does not support polling"
        );
    }
    
    /**
     * Retrieve final results from an async scan job.
     */
    @NonNull
    default Result fetchResults(@NonNull Submission submission) throws ScannerException {
        throw new UnsupportedOperationException(
            "Scanner " + getScannerType() + " does not support result retrieval"
        );
    }
}
