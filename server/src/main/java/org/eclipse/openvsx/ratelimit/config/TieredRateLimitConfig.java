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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.eclipse.openvsx.ratelimit.CustomerService;
import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.eclipse.openvsx.ratelimit.IdentityService;
import org.eclipse.openvsx.ratelimit.filter.TieredRateLimitServletFilterFactory;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(value = "ovsx.tiered-rate-limit.enabled", havingValue = "true")
@ConditionalOnBean(JedisCluster.class)
public class TieredRateLimitConfig {

    private final Logger logger = LoggerFactory.getLogger(TieredRateLimitConfig.class);

    public static final String CACHE_RATE_LIMIT_CUSTOMER = "ratelimit.customer";
    public static final String CACHE_RATE_LIMIT_TOKEN = "ratelimit.token";

    @Bean
    UsageDataService usageDataService(RepositoryService repositories, CustomerService customerService, JedisCluster jedisCluster) {
        return new UsageDataService(repositories, customerService, jedisCluster);
    }

    @Bean
    ServletRateLimiterFilterFactory tieredServletFilterFactory(
        UsageDataService
        customerUsageService,
        IdentityService identityService
    ) {
        return new TieredRateLimitServletFilterFactory(customerUsageService, identityService);
    }

    @Bean
    public Cache<Object, Object> customerCache(
            @Value("${ovsx.caching.customer.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.customer.max-size:10000}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .expireAfterAccess(timeToIdle)
                .maximumSize(maxSize)
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Object, Object> tokenCache(
            @Value("${ovsx.caching.token.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.token.max-size:10000}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .expireAfterAccess(timeToIdle)
                .maximumSize(maxSize)
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier("rateLimitCacheManager")
    public CacheManager rateLimitCacheManager(
            Cache<Object, Object> customerCache,
            Cache<Object, Object> tokenCache
    ) {
        logger.info("Configure rate limit cache manager");
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.registerCustomCache(CACHE_RATE_LIMIT_CUSTOMER, customerCache);
        caffeineCacheManager.registerCustomCache(CACHE_RATE_LIMIT_TOKEN, tokenCache);

        return caffeineCacheManager;
    }
}
