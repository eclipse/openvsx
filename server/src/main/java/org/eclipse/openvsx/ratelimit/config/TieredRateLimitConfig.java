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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(prefix = TieredRateLimitProperties.PROPERTY_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties({TieredRateLimitProperties.class})
public class TieredRateLimitConfig  {

    private final Logger logger = LoggerFactory.getLogger(TieredRateLimitConfig.class);

    public static final String CACHE_RATE_LIMIT_CUSTOMER = "ratelimit.customer";
    public static final String CACHE_RATE_LIMIT_TOKEN = "ratelimit.token";
    public static final String CACHE_RATE_LIMIT_BUCKET = "ratelimit.bucket";

    @Bean
    public JedisCluster jedisCluster(RedisProperties properties) {
        logger.info("Configure jedis-cluster rate-limiting cache");
        var configBuilder = DefaultJedisClientConfig.builder();
        var username = properties.getUsername();
        if(StringUtils.isNotEmpty(username)) {
            configBuilder.user(username);
        }
        var password = properties.getPassword();
        if(StringUtils.isNotEmpty(password)) {
            configBuilder.password(password);
        }

        var nodes = properties.getCluster().getNodes().stream()
                .map(HostAndPort::from)
                .collect(Collectors.toSet());

        return new JedisCluster(nodes, configBuilder.build());
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
    public Cache<Object, Object> bucketCache(
            @Value("${ovsx.caching.bucket.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.bucket.max-size:10000}") long maxSize
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
            Cache<Object, Object> tokenCache,
            Cache<Object, Object> bucketCache
    ) {
        logger.info("Configure rate limit cache manager");
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.registerCustomCache(CACHE_RATE_LIMIT_CUSTOMER, customerCache);
        caffeineCacheManager.registerCustomCache(CACHE_RATE_LIMIT_TOKEN, tokenCache);
        caffeineCacheManager.registerCustomCache(CACHE_RATE_LIMIT_BUCKET, bucketCache);

        return caffeineCacheManager;
    }

    @Bean
    @ConditionalOnMissingBean(ProxyManager.class)
    public ProxyManager<byte[]> jedisBasedProxyManager(JedisCluster jedisCluster) {
        return JedisBasedProxyManager.builderFor(jedisCluster)
                .withClientSideConfig(
                        ClientSideConfig
                                .getDefault()
                                .withExpirationAfterWriteStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))))
                .build();
    }
}
