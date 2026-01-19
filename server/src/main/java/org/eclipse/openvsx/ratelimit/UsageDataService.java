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

import org.eclipse.openvsx.entities.Customer;
import org.eclipse.openvsx.entities.UsageStats;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class UsageDataService {
    private final static String USAGE_DATA_KEY = "usage.customer";
    private final static int WINDOW_MINUTES = 5;

    private final Logger logger = LoggerFactory.getLogger(UsageDataService.class);

    private final RepositoryService repositories;
    private final CustomerService customerService;
    private final JedisCluster jedisCluster;

    public UsageDataService(RepositoryService repositories, CustomerService customerService, JedisCluster jedisCluster) {
        this.repositories = repositories;
        this.customerService = customerService;
        this.jedisCluster = jedisCluster;
    }

    public void incrementUsage(Customer customer) {
        var key = customer.getId();
        var window = getCurrentUsageWindow();
        var old = jedisCluster.hincrBy(USAGE_DATA_KEY, key + ":" + window, 1);
        logger.info("Usage count for {}: {}", customer.getName(), old + 1);
    }

    public void persistUsageStats() {
        var currentWindow = getCurrentUsageWindow();

        String cursor = ScanParams.SCAN_POINTER_START;
        ScanResult<Map.Entry<String, String>> results;

        do {
            results = jedisCluster.hscan(USAGE_DATA_KEY, cursor);

            for (var result : results.getResult()) {
                var key = result.getKey();
                var value = result.getValue();

                logger.info("{} - {}", key, value);

                var component = key.split(":");
                var customerId = Long.parseLong(component[0]);
                var window = Long.parseLong(component[1]);

                if (window < currentWindow) {
                    var customer = customerService.getCustomerById(customerId);
                    if (customer.isEmpty()) {
                        logger.warn("failed to find customer with id {}", customerId);
                    } else {
                        UsageStats stats = new UsageStats();

                        stats.setCustomer(customer.get());
                        stats.setWindowStart(LocalDateTime.ofInstant(Instant.ofEpochSecond(window * 60), ZoneOffset.UTC));
                        stats.setCount(Long.parseLong(value));
                        stats.setDuration(Duration.ofMinutes(WINDOW_MINUTES));
                        repositories.saveUsageStats(stats);
                    }

                    jedisCluster.hdel(USAGE_DATA_KEY, key);
                }
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
