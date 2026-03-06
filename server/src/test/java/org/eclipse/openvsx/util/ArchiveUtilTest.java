/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.PATH;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

class ArchiveUtilTest {

    @Test
    void testTodoTree() throws Exception {
        var packageUrl = getClass().getResource("todo-tree.zip");

        assertThat(packageUrl).isNotNull();
        assertThat(packageUrl.getProtocol()).isEqualTo("file");

        try (
            var archive = new ZipFile(packageUrl.getPath());
            var packageFile = ArchiveUtil.readEntry(archive, "extension/package.json");
            var iconFile = ArchiveUtil.readEntry(archive, "extension/resources/todo-tree.png")
        ) {
            assertThat(packageFile).isNotNull();
            assertThat(Files.size(packageFile.getPath())).isEqualTo(44712);
            assertThat(iconFile).isNotNull();
            assertThat(Files.size(iconFile.getPath())).isEqualTo(8854);
        }
    }

    @Test
    void testExceedMaxEntrySize() throws IOException {
        // an artificially crafted zip file with a file whose size is set lower as its actual content
        var packageUrl = getClass().getResource("wrong-size.zip");

        assertThat(packageUrl).isNotNull();
        assertThat(packageUrl.getProtocol()).isEqualTo("file");

        try (var archive = new ZipFile(packageUrl.getPath())) {
            assertThatThrownBy(() -> ArchiveUtil.readEntry(archive, "extension/README.md", 8192))
                    .isExactlyInstanceOf(ErrorResultException.class)
                    .hasMessage("The file extension/README.md exceeds the size limit of 8 KB.");

            assertThatThrownBy(() -> ArchiveUtil.readEntry(archive, "extension/package.json", 8192))
                    .isExactlyInstanceOf(ErrorResultException.class)
                    .hasMessageContaining("Failed to read extension/package.json: File size exceeds limit of 0 bytes.");
        }
    }
}