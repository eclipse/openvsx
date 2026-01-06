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

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the overall result of a secret scan.
 * Immutable so callers cannot mutate findings after creation.
 */
public final class SecretScanResult {

    private final boolean secretsFound;
    private final @NotNull List<SecretFinding> findings;

    private SecretScanResult(boolean secretsFound, @NotNull List<SecretFinding> findings) {
        this.secretsFound = secretsFound;
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    /**
     * Factory for a result when secrets are found.
     */
    public static SecretScanResult secretsFound(@NotNull List<SecretFinding> findings) {
        if (findings.isEmpty()) {
            throw new IllegalArgumentException("Cannot create secretsFound result with empty findings");
        }
        return new SecretScanResult(true, findings);
    }

    /**
     * Factory for a result when no secrets are present.
     */
    public static SecretScanResult noSecretsFound() {
        return new SecretScanResult(false, List.of());
    }

    /**
     * Factory for a result when scanning was skipped.
     */
    public static SecretScanResult skipped() {
        return new SecretScanResult(false, List.of());
    }

    public boolean isSecretsFound() {
        return secretsFound;
    }

    public @NotNull List<SecretFinding> getFindings() {
        return findings;
    }

    public @NotNull String getSummaryMessage() {
        if (!secretsFound) {
            return "No secrets detected";
        }

        return String.format(
            "Found %d potential secret%s in extension package",
            findings.size(),
            findings.size() == 1 ? "" : "s"
        );
    }
}

