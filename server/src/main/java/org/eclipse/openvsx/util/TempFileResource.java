/** ******************************************************************************
 * Copyright (c) 2026 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;

/**
 * A {@link FileSystemResource} backed by a {@link TempFile} that deletes the temp file once the
 * response body has been written, i.e. when the message converter closes the input stream.
 */
public class TempFileResource extends FileSystemResource {

    private final TempFile tempFile;

    public TempFileResource(TempFile tempFile) {
        super(tempFile.getPath());
        this.tempFile = tempFile;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FilterInputStream(super.getInputStream()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    IOUtils.closeQuietly(TempFileResource.this.tempFile);
                }
            }
        };
    }
}
