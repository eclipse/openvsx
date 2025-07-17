/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import com.giffing.bucket4j.spring.boot.starter.context.properties.Bucket4JBootProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.cache.CacheService.*;

@Configuration
@EnableCaching(proxyTargetClass = true)
public class CacheConfig {

    @Bean
    public Cache<Object, Object> extensionCache(
            @Value("${ovsx.caching.files-extension.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.files-extension.max-size:20}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .removalListener(new ExpiredFileListener())
                .expireAfterAccess(timeToIdle)
                .maximumSize(maxSize)
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .build();
    }

    @Bean
    public Cache<Object, Object> webResourceCache(
            @Value("${ovsx.caching.files-webresource.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.files-webresource.max-size:150}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .removalListener(new ExpiredFileListener())
                .expireAfterAccess(timeToIdle)
                .maximumSize(maxSize)
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .build();
    }

    @Bean
    public CacheManager fileCacheManager(Cache<Object, Object> extensionCache, Cache<Object, Object> webResourceCache) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.registerCustomCache(CACHE_EXTENSION_FILES, extensionCache);
        caffeineCacheManager.registerCustomCache(CACHE_WEB_RESOURCE_FILES, webResourceCache);
        return caffeineCacheManager;
    }

    @Bean
    @ConditionalOnProperty(prefix = Bucket4JBootProperties.PROPERTY_PREFIX, name = "cache-to-use", havingValue = "redis-jedis")
    public JedisPool jedisPool(RedisProperties properties) {
        return new JedisPool(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword());
    }

    @Bean
    @ConditionalOnProperty(prefix = Bucket4JBootProperties.PROPERTY_PREFIX, name = "cache-to-use", havingValue = "redis-cluster-jedis")
    public JedisCluster jedisCluster(RedisProperties properties) {
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
    @Primary
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            @Value("${ovsx.caching.average-review-rating.ttl:P1000D}") Duration averageReviewRatingTtl,
            @Value("${ovsx.caching.namespace-details-json.ttl:PT1H}") Duration namespaceDetailsJsonTtl,
            @Value("${ovsx.caching.database-search.ttl:PT1H}") Duration databaseSearchTtl,
            @Value("${ovsx.caching.extension-json.ttl:PT1H}") Duration extensionJsonTtl,
            @Value("${ovsx.caching.latest-extension-version.ttl:PT1H}") Duration latestExtensionVersionTtl,
            @Value("${ovsx.caching.buckets.ttl:PT1H}") Duration bucketsTtl,
            @Value("${ovsx.caching.sitemap.ttl:PT1H}") Duration sitemapTtl,
            @Value("${ovsx.caching.malicious-extensions.ttl:P1D}") Duration maliciousExtensionsTtl
    ) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration(CACHE_AVERAGE_REVIEW_RATING, redisCacheConfig().entryTtl(averageReviewRatingTtl))
                .withCacheConfiguration(CACHE_NAMESPACE_DETAILS_JSON, redisCacheConfig().entryTtl(namespaceDetailsJsonTtl))
                .withCacheConfiguration(CACHE_DATABASE_SEARCH, redisCacheConfig().entryTtl(databaseSearchTtl))
                .withCacheConfiguration(CACHE_EXTENSION_JSON, redisCacheConfig().entryTtl(extensionJsonTtl))
                .withCacheConfiguration(CACHE_LATEST_EXTENSION_VERSION, redisCacheConfig().entryTtl(latestExtensionVersionTtl))
                .withCacheConfiguration("buckets", redisCacheConfig().entryTtl(bucketsTtl))
                .withCacheConfiguration(CACHE_SITEMAP, redisCacheConfig().entryTtl(sitemapTtl))
                .withCacheConfiguration(CACHE_MALICIOUS_EXTENSIONS, redisCacheConfig().entryTtl(maliciousExtensionsTtl))
                .build();
    }

    private RedisCacheConfiguration redisCacheConfig() {
        var serializationPair = RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());
        return RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializationPair);
    }
}
