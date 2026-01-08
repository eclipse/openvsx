/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation 
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

import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.eclipse.openvsx.util.ErrorResultException;
import org.springframework.http.HttpStatus;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Service for scanning extension packages for potential secrets before publication.
 * This service analyzes VSIX files to detect potential secrets like API keys, tokens,
 * passwords, and other sensitive credentials that should not be published publicly.
 * 
 * Uses Spring's default async executor for parallel file scanning within extension packages.
 */
@Service
public class SecretScanningService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretScanningService.class);
    
    private final SecretScanningConfig config;
    private final SecretScanner fileContentScanner;
    private final AsyncTaskExecutor taskExecutor;

    private final int maxEntryCount;
    private final long maxTotalUncompressedBytes;
    private final int maxFindings;
    
    /**
     * Exception thrown when scan is cancelled due to finding limits or other constraints.
     */
    static class ScanCancelledException extends RuntimeException {
        ScanCancelledException(String message) { super(message); }
    }
    
    /**
     * Constructs a secret scanning service with the specified configuration and executor.
     */
    public SecretScanningService(
            SecretScanningConfig config,
            SecretScannerFactory scannerFactory,
            AsyncTaskExecutor taskExecutor) {
        this.config = config;
        this.taskExecutor = taskExecutor;
        
        // Cache configuration values that are reused during scanning
        this.maxEntryCount = config.getMaxEntryCount();
        this.maxTotalUncompressedBytes = config.getMaxTotalUncompressedBytes();
        this.maxFindings = config.getMaxFindings();

        this.fileContentScanner = scannerFactory.getScanner();
    }
    
    /**
     * Returns whether secret scanning is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    /**
     * Scans an extension package for potential secrets.
     * 
     * Callers should check {@link #isEnabled()} before invoking this method.
     */
    public SecretScanResult scanForSecrets(@NotNull TempFile extensionFile) {
        // Use thread-safe collection for parallel processing
        List<SecretFinding> findings = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger findingsCount = new AtomicInteger(0); // Cap findings to protect memory
        
        try (ZipFile zipFile = new ZipFile(extensionFile.getPath().toFile())) {
            List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
            ArchiveUtil.enforceArchiveLimits(entries, maxEntryCount, maxTotalUncompressedBytes);
            
            AtomicInteger filesScanned = new AtomicInteger(0);
            AtomicInteger filesSkipped = new AtomicInteger(0);
            
            long startTime = System.currentTimeMillis();
            long timeoutMillis = config.getTimeoutSeconds() * 1000L;
            List<Future<?>> tasks = new ArrayList<>(entries.size());
            
            try {
                for (ZipEntry entry : entries) {
                    tasks.add(taskExecutor.submit(() -> {
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            throw new SecretScanningTimeoutException("Secret scanning timed out");
                        }
                        
                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }

                        if (entry.isDirectory()) {
                            return null;
                        }
                        
                        String filePath = entry.getName();
                        try {
                            boolean scanned = fileContentScanner.scanFile(
                                zipFile,
                                entry,
                                findings,
                                startTime,
                                timeoutMillis,
                                findingsCount,
                                this::recordFinding
                            );
                            if (scanned) {
                                filesScanned.incrementAndGet();
                            } else {
                                filesSkipped.incrementAndGet();
                            }
                        } catch (SecretScanningTimeoutException | ScanCancelledException e) {
                            throw e;
                        } catch (Exception e) {
                            logger.warn("Failed to scan file {}: {}", filePath, e.getMessage());
                        }
                        return null;
                    }));
                }
                
                for (Future<?> task : tasks) {
                    try {
                        task.get();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        if (cause instanceof SecretScanningTimeoutException) {
                            throw (SecretScanningTimeoutException) ee.getCause();
                        } else if (cause instanceof ScanCancelledException) {
                            break;
                        }
                    }
                }
            } finally {
                // Ensure we cancel any remaining tasks if the loop exits early
                for (Future<?> f : tasks) {
                    f.cancel(true);
                }
            }
            
            logger.debug("Secret scan complete: {} files scanned, {} files skipped, {} findings", 
                        filesScanned.get(), filesSkipped.get(), findings.size());
            
        } catch (SecretScanningTimeoutException e) {
            logger.error("Secret scanning timed out after {} seconds", config.getTimeoutSeconds());
            throw new ErrorResultException(
                    "Secret scanning timed out after " + config.getTimeoutSeconds()
                            + " seconds. Please reduce the file size or exclude large files.",
                    HttpStatus.REQUEST_TIMEOUT);
        } catch (ZipException e) {
            logger.error("Failed to open extension file as zip: {}", e.getMessage());
            throw new ErrorResultException("Failed to scan extension file: invalid zip format", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            logger.error("Failed to scan extension file: {}", e.getMessage());
            throw new ErrorResultException("Failed to scan extension file", HttpStatus.BAD_REQUEST);
        }
        
        if (findings.isEmpty()) {
            return SecretScanResult.noSecretsFound();
        } else {
            return SecretScanResult.secretsFound(findings);
        }
    }
    
    /**
     * Record a finding while respecting the global cap.
     */
    private boolean recordFinding(List<SecretFinding> findings, AtomicInteger findingsCount,
                                  SecretFinding finding) {
        int newCount = findingsCount.incrementAndGet();
        if (newCount > maxFindings) {
            throw new ScanCancelledException("Max findings reached");
        }
        findings.add(finding);
        return true;
    }
}

/**
 * Signals that a scan exceeded the configured timeout budget.
 */
class SecretScanningTimeoutException extends RuntimeException {
    SecretScanningTimeoutException(String message) {
        super(message);
    }
}

