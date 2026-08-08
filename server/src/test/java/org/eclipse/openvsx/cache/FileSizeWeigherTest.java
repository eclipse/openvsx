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
package org.eclipse.openvsx.cache;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileSizeWeigherTest {

    private final FileSizeWeigher weigher = new FileSizeWeigher();

    @Test
    void testWeighsByFileSize(@TempDir Path tmpDir) throws Exception {
        var path = tmpDir.resolve("resource.bin");
        Files.write(path, new byte[1234]);

        assertThat(weigher.weigh("key", path)).isEqualTo(1234);
    }

    @Test
    void testWeighsMissingFileAsMax() {
        var path = Path.of("/does/not/exist");

        assertThat(weigher.weigh("key", path)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void testWeighsNonPathValueAsOne() {
        assertThat(weigher.weigh("key", "not-a-path")).isEqualTo(1);
    }
}
