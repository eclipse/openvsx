/*
 * Copyright (c) 2026 Eclipse Foundation AISBL
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.openvsx.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class NormalizedZipFileTest {

    private File createZip(String... entryNames) throws IOException {
        var file = File.createTempFile("nzf-test", ".zip");
        file.deleteOnExit();
        try (var zos = new ZipOutputStream(new FileOutputStream(file))) {
            for (var name : entryNames) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(new byte[0]);
                zos.closeEntry();
            }
        }
        return file;
    }

    @Test
    void backslashEntryFoundViaForwardSlashLookup() throws IOException {
        var zip = createZip("extension\\package.json");
        try (var nzf = new NormalizedZipFile(zip)) {
            assertThat(nzf.getEntry("extension/package.json")).isNotNull();
            assertThat(nzf.hasDuplicateNormalizedEntries()).isFalse();
        }
    }

    @Test
    void duplicateAfterNormalizationIsDetected() throws IOException {
        var zip = createZip("extension/package.json", "extension\\package.json");
        try (var nzf = new NormalizedZipFile(zip)) {
            assertThat(nzf.hasDuplicateNormalizedEntries()).isTrue();
        }
    }

    @Test
    void noDuplicatesForCleanZip() throws IOException {
        var zip = createZip("extension/package.json", "extension/README.md");
        try (var nzf = new NormalizedZipFile(zip)) {
            assertThat(nzf.hasDuplicateNormalizedEntries()).isFalse();
        }
    }

    @Test
    void getEntryIgnoreCaseWithBackslash() throws IOException {
        var zip = createZip("Extension\\Package.JSON");
        try (var nzf = new NormalizedZipFile(zip)) {
            assertThat(nzf.getEntryIgnoreCase("extension/package.json")).isNotNull();
        }
    }

    @Test
    void entriesReturnsAllOriginalEntries() throws IOException {
        var zip = createZip("extension/package.json", "extension\\README.md");
        try (var nzf = new NormalizedZipFile(zip)) {
            var entries = nzf.entries();
            assertThat(entries).hasSize(2);
            assertThat(entries.stream().map(ZipEntry::getName)).containsExactlyInAnyOrder("extension/package.json", "extension\\README.md");
        }
    }
}
