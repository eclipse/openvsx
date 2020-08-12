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

import com.google.common.io.ByteStreams;

import org.junit.jupiter.api.Test;

public class ArchiveUtilTest {

    @Test
    public void testTodoTree() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("todo-tree.zip");
        ) {
            var bytes = ByteStreams.toByteArray(stream);
            var packageJson = ArchiveUtil.readEntry(bytes, "extension/package.json");
            assertThat(packageJson.length).isEqualTo(24052);
            var icon = ArchiveUtil.readEntry(bytes, "extension/resources/todo-tree.png");
            assertThat(icon.length).isEqualTo(8854);
        }
    }

}