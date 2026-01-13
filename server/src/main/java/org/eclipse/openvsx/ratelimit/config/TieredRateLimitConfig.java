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
package org.eclipse.openvsx.ratelimit.config;

import com.giffing.bucket4j.spring.boot.starter.filter.servlet.ServletRateLimiterFilterFactory;
import org.eclipse.openvsx.ratelimit.CustomerUsageService;
import org.eclipse.openvsx.ratelimit.filter.TieredRateLimitServletFilterFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisCluster;

@Configuration
@ConditionalOnProperty(value = "ovsx.tiered-rate-limit.enabled", havingValue = "true")
@ConditionalOnBean(JedisCluster.class)
public class TieredRateLimitConfig {

    @Bean
    CustomerUsageService customerUsageService(JedisCluster jedisCluster) {
        return new CustomerUsageService(jedisCluster);
    }

    @Bean
    ServletRateLimiterFilterFactory tieredServletFilterFactory(CustomerUsageService customerUsageService) {
        return new TieredRateLimitServletFilterFactory(customerUsageService);
    }
}
