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
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Interface for validation checks that run during extension publishing.
 * 
 * Implementations are auto-discovered by Spring and executed by ExtensionScanService.
 * To add a new validation check, create a @Component that implements this interface.
 */
public interface ValidationCheck {

    /**
     * Unique identifier for this check type.
     * Used for recording failures and filtering in the admin UI.
     */
    String getCheckType();

    /**
     * Whether this check is enabled. Disabled checks are skipped entirely.
     */
    boolean isEnabled();

    /**
     * Whether failures from this check should block publication.
     * Non-enforced checks record failures for monitoring but allow publication to proceed.
     */
    boolean isEnforced();

    /**
     * Execute the validation check.
     */
    Result check(Context context);

    /**
     * Context passed to validation checks during extension publishing.
     * 
     * Contains the scan record (with extension metadata), the extension file,
     * and the publishing user. Use scan.getNamespaceName(), scan.getExtensionName(),
     * etc. to access extension metadata.
     */
    record Context(
        @NonNull ExtensionScan scan,
        @NonNull TempFile extensionFile,
        @NonNull UserData user
    ) {
        public Context {
            if (scan == null) {
                throw new IllegalArgumentException("scan cannot be null");
            }
            if (extensionFile == null) {
                throw new IllegalArgumentException("extensionFile cannot be null");
            }
            if (user == null) {
                throw new IllegalArgumentException("user cannot be null");
            }
        }
    }

    /**
     * Result of a validation check execution.
     */
    record Result(
        boolean passed,
        List<Failure> failures
    ) {
        public static Result pass() {
            return new Result(true, List.of());
        }

        public static Result fail(List<Failure> failures) {
            return new Result(false, failures);
        }

        public static Result fail(String ruleName, String reason) {
            return new Result(false, List.of(new Failure(ruleName, reason)));
        }
    }

    /**
     * A single validation failure with details for recording.
     */
    record Failure(
        String ruleName,
        String reason
    ) {}
}
