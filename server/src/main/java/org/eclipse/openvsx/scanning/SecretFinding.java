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
import javax.annotation.Nullable;
/**
 * Represents a single finding discovered during secret scanning.
 * Secrets are redacted immediately to avoid accidental leakage.
 */
public final class SecretFinding {

    private final @NotNull String filePath;
    private final int lineNumber;
    private final double entropy;
    private final @NotNull String redactedSecret;
    private final @NotNull String ruleId;

    public SecretFinding(@NotNull String filePath,
                         int lineNumber,
                         double entropy,
                         @Nullable String secretValue,
                         @NotNull String ruleId) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.entropy = entropy;
        this.redactedSecret = redactSecret(secretValue);
        this.ruleId = ruleId;
    }

    public @NotNull String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public double getEntropy() {
        return entropy;
    }

    public @NotNull String getSecretValue() {
        return redactedSecret;
    }

    public @NotNull String getRuleId() {
        return ruleId;
    }

    @Override
    public String toString() {
        return String.format(
            "Potential secret found in %s:%d (entropy: %.2f, rule: %s): %s",
            filePath,
            lineNumber,
            entropy,
            ruleId,
            redactedSecret
        );
    }

    private static @NotNull String redactSecret(@Nullable String secret) {
        if (secret == null || secret.length() <= 6) {
            return "***";
        }

        // Show first 3 and last 3 characters to not leak content.
        int prefixLen = Math.min(3, secret.length() / 3);
        int suffixLen = Math.min(3, secret.length() / 3);

        String prefix = secret.substring(0, prefixLen);
        String suffix = secret.substring(secret.length() - suffixLen);

        return prefix + "***" + suffix;
    }
}

