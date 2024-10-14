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

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFile implements AutoCloseable {

    private final Path path;
    private FileResource resource;
    private Namespace namespace;

    public TempFile(String prefix, String suffix) throws IOException {
        path = Files.createTempFile(prefix, suffix);
    }

    public Path getPath() {
        return path;
    }

    public FileResource getResource() {
        return resource;
    }

    public void setResource(FileResource resource) {
        this.resource = resource;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public void close() throws IOException {
        Files.delete(path);
    }
}
