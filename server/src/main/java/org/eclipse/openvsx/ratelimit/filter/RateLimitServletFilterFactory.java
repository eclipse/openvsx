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
package org.eclipse.openvsx.ratelimit.filter;

import com.giffing.bucket4j.spring.boot.starter.context.properties.FilterConfiguration;
import com.giffing.bucket4j.spring.boot.starter.filter.servlet.ServletRateLimitFilter;
import com.giffing.bucket4j.spring.boot.starter.filter.servlet.ServletRateLimiterFilterFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.cache.CacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

@Component
@ConditionalOnProperty(value = "ovsx.rate-limit.enabled", havingValue = "true")
@ConditionalOnBean(JedisCluster.class)
public class RateLimitServletFilterFactory implements ServletRateLimiterFilterFactory {
    private final CacheService cacheService;

    public RateLimitServletFilterFactory(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public ServletRateLimitFilter create(FilterConfiguration<HttpServletRequest, HttpServletResponse> filterConfig) {
        return new RateLimitServletFilter(cacheService, filterConfig);
    }
}
