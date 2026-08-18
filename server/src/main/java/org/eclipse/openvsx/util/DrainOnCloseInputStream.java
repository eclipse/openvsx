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

import com.google.common.io.ByteStreams;

/**
 * InputStream wrapper that drains up to a fixed number of remaining unread bytes before closing.
 * <p>
 * A request body that a validation check rejects before the body has been fully read can make
 * some load balancers/proxies mistake the resulting early connection close for a server error.
 * Wrapping a request InputStream once at the entry point and reading it exclusively through
 * try-with-resources makes draining automatic on every exit path - success, an early validation
 * throw, or anything added later - instead of relying on each call site remembering to drain
 * manually before it responds.
 * <p>
 * The <b>total</b> bytes ever read from the underlying stream over this instance's lifetime -
 * whatever a caller reads normally before rejecting, plus whatever close() then drains - is capped
 * at {@code maxBytes}: without a bound, a client rejected for any reason (invalid token, an
 * oversized upload, ...) could force the server to read and discard an arbitrarily large body
 * before responding, trading the original LB/proxy problem for a resource-exhaustion one.
 * {@code maxBytes} should be the same "reasonable size" limit the caller already accepts reading
 * in full on the success path (e.g. the configured max upload size) - draining up to that bound
 * costs nothing beyond what a legitimate request already costs, while anything beyond it is by
 * definition not a legitimate request, and is left undrained.
 */
public class DrainOnCloseInputStream extends FilterInputStream {

    private final long maxBytes;
    private long bytesRead = 0;

    public DrainOnCloseInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            bytesRead += n;
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        try {
            long remaining = maxBytes - bytesRead;
            if (remaining > 0) {
                ByteStreams.limit(this, remaining).transferTo(OutputStream.nullOutputStream());
            }
        } catch (IOException ignored) {
            // best-effort drain; connection state doesn't matter once we're closing this stream
        } finally {
            super.close();
        }
    }
}
