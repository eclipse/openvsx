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
package org.eclipse.openvsx.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream wrapper that drains any remaining unread bytes before closing.
 * <p>
 * A request body that a validation check rejects before the body has been fully read can make
 * some load balancers/proxies mistake the resulting early connection close for a server error.
 * Wrapping a request InputStream once at the entry point and reading it exclusively through
 * try-with-resources makes draining automatic on every exit path - success, an early validation
 * throw, or anything added later - instead of relying on each call site remembering to drain
 * manually before it responds.
 */
public class DrainOnCloseInputStream extends FilterInputStream {

    public DrainOnCloseInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        try {
            transferTo(OutputStream.nullOutputStream());
        } catch (IOException ignored) {
            // best-effort drain; connection state doesn't matter once we're closing this stream
        } finally {
            super.close();
        }
    }
}
