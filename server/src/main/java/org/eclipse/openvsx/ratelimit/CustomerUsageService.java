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

import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Instant;
import java.util.Map;

public class CustomerUsageService {
    private final static String USAGE_KEY = "usage";
    private final static int WINDOW_MINUTES = 5;

    private final Logger logger = LoggerFactory.getLogger(CustomerUsageService.class);

    private final RepositoryService repositories;
    private final JedisCluster jedisCluster;

    public CustomerUsageService(RepositoryService repositories, JedisCluster jedisCluster) {
        this.repositories = repositories;
        this.jedisCluster = jedisCluster;
    }

    public void incrementUsage(String key) {
        var window = getCurrentUsageWindow();
        var old = jedisCluster.hincrBy(USAGE_KEY, key + ":" + window, 1);
        logger.info("Usage count for {}: {}", key, old + 1);
    }

    public void persistUsageStats() {
        var window = getCurrentUsageWindow();

        String cursor = ScanParams.SCAN_POINTER_START;
        ScanResult<Map.Entry<String, String>> results;

        do {
            results = jedisCluster.hscan(USAGE_KEY, cursor);

            for (var result : results.getResult()) {
                logger.info("{} - {}", result.getKey(), result.getValue());
            }

            cursor = results.getCursor();
        } while (!results.isCompleteIteration());
    }

    private long getCurrentUsageWindow() {
        var instant = Instant.now();
        var epochMinute = instant.getEpochSecond() / 60;
        return epochMinute / WINDOW_MINUTES * WINDOW_MINUTES;
    }
}
