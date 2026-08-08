/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.benmanes.caffeine.cache.Weigher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Weighs a cached {@link Path} by its file size in bytes, so a {@code maximumWeight} cache bounds
 * its total disk footprint instead of just the number of cached files.
 */
public class FileSizeWeigher implements Weigher<Object, Object> {
    private static final Logger logger = LoggerFactory.getLogger(FileSizeWeigher.class);

    @Override
    public int weigh(@NonNull Object key, @NonNull Object value) {
        if (!(value instanceof Path path)) {
            return 1;
        }

        try {
            return (int) Math.min(Files.size(path), Integer.MAX_VALUE);
        } catch (IOException e) {
            // Can't determine the size, e.g. the file was already deleted; weigh it heavily so
            // it doesn't linger and displace entries whose size is known.
            logger.warn("Failed to determine size of cached file {}", path, e);
            return Integer.MAX_VALUE;
        }
    }
}
