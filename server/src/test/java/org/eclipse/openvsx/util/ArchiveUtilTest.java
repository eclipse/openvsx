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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

class ArchiveUtilTest {

    @Test
    void testTodoTree() throws Exception {
        var packageUrl = getClass().getResource("todo-tree.zip");
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

}