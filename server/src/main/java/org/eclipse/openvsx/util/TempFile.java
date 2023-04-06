/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFile implements AutoCloseable {

    private final Path path;

    public TempFile(String prefix, String suffix) throws IOException {
        path = Files.createTempFile(prefix, suffix);
    }

    public Path getPath() {
        return path;
    }

    @Override
    public void close() throws IOException {
        Files.delete(path);
    }
}
