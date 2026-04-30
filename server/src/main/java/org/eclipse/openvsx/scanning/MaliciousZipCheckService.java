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

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

@Service
@Order(0)
public class MaliciousZipCheckService implements PublishCheck {

    public static final String CHECK_TYPE = "MALICIOUS_ZIP_CHECK";
    private static final String EXTRA_FIELDS_RULE = "EXTRA_FIELDS_DETECTED";
    private static final String EXTRA_FIELDS_MESSAGE = "extension file contains zip entries with potentially harmful extra fields";
    private static final String DUPLICATE_ENTRIES_RULE = "DUPLICATE_NORMALIZED_ENTRIES";
    private static final String DUPLICATE_ENTRIES_MESSAGE = "extension file contains duplicate zip entries after path normalization";

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
        return failures
                .stream()
                .map(f -> switch (f.ruleName()) {
                    case EXTRA_FIELDS_RULE -> "Extension contains zip entries with unsupported extra fields";
                    case DUPLICATE_ENTRIES_RULE -> "Extension contains duplicate zip entries after path normalization";
                    default -> f.reason();
                })
                .collect(Collectors.joining("; "));
    }

    @Override
    public PublishCheck.Result check(Context context) {
        try (var zipFile = new ZipFile(context.extensionFile().getPath().toFile())) {
            return checkForExtraFields(zipFile)
                    .and(checkForDuplicateEntries(zipFile));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read extension zip file", e);
        }
    }

    private PublishCheck.Result checkForExtraFields(ZipFile zipFile) {
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getExtra() != null) {
                return PublishCheck.Result.fail(EXTRA_FIELDS_RULE, EXTRA_FIELDS_MESSAGE);
            }
        }
        return PublishCheck.Result.pass();
    }

    private PublishCheck.Result checkForDuplicateEntries(ZipFile zipFile) {
        var seen = new HashSet<String>();
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            var raw = entries.nextElement().getName();
            var name = Path.of(raw).normalize().toString();
            if (!seen.add(name.replace('\\', '/'))) {
                return PublishCheck.Result.fail(DUPLICATE_ENTRIES_RULE, DUPLICATE_ENTRIES_MESSAGE);
            }
        }
        return PublishCheck.Result.pass();
    }
}
