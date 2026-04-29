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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipFile;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Service for checking extension files for duplicate zip entries after path normalization.
 *
 * yauzl (used by VS Code) normalizes backslashes to forward slashes when reading zip entries.
 *
 */
@Service
@Order(1)
public class DuplicateZipEntriesCheckService implements PublishCheck {

    public static final String CHECK_TYPE = "DUPLICATE_ZIP_ENTRIES_CHECK";
    private static final String RULE_NAME = "DUPLICATE_NORMALIZED_ENTRIES";
    private static final String MESSAGE = "extension file contains duplicate zip entries after path normalization";
    private static final String USER_MESSAGE = "Extension contains duplicate zip entries after path normalization";

    @Override
    public String getCheckType() {
        return CHECK_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isEnforced() {
        return true;
    }

    @Override
    public String getUserFacingMessage(List<Failure> failures) {
        return USER_MESSAGE;
    }

    @Override
    public PublishCheck.Result check(Context context) {
        try (var zipFile = new ZipFile(context.extensionFile().getPath().toFile())) {
            var seen = new HashSet<String>();
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                var raw = entries.nextElement().getName().replace('\\', '/');
                var name = Path.of(raw).normalize().toString().replace('\\', '/');
                if (!seen.add(name)) {
                    return PublishCheck.Result.fail(RULE_NAME, MESSAGE);
                }
            }
        } catch (IOException e) {
            throw new DuplicateZipEntriesCheckException("Failed to read extension zip file", e);
        }

        return PublishCheck.Result.pass();
    }
}

/**
 * Signals that the duplicate-zip-entries check could not be executed (e.g. the archive could not be read).
 */
class DuplicateZipEntriesCheckException extends RuntimeException {
    DuplicateZipEntriesCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
