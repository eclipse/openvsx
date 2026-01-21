/*
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
 */
package org.eclipse.openvsx.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

import java.nio.charset.StandardCharsets;

public class TieredRateLimitService {
    private final ProxyManager<byte[]> proxyManager;

    public TieredRateLimitService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public Bucket getBucket(String key, BucketConfiguration bucketConfiguration) {
        Bucket bucket = proxyManager.builder().build(key.getBytes(StandardCharsets.UTF_8), bucketConfiguration);
        return bucket;
    }
}
