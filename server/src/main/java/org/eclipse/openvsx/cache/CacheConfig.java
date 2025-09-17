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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.giffing.bucket4j.spring.boot.starter.context.properties.Bucket4JBootProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.micrometer.common.util.StringUtils;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceDetailsJson;
import org.eclipse.openvsx.search.SearchResult;
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
import org.springframework.data.redis.serializer.*;
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
    public Cache<Object, Object> browseCache(
            @Value("${ovsx.caching.files-browse.tti:PT1H}") Duration timeToIdle,
            @Value("${ovsx.caching.files-browse.max-size:50}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .expireAfterAccess(timeToIdle)
                .maximumSize(maxSize)
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .build();
    }

    @Bean
    public CacheManager fileCacheManager(
            Cache<Object, Object> extensionCache,
            Cache<Object, Object> webResourceCache,
            Cache<Object, Object> browseCache
    ) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.registerCustomCache(CACHE_EXTENSION_FILES, extensionCache);
        caffeineCacheManager.registerCustomCache(CACHE_WEB_RESOURCE_FILES, webResourceCache);
        caffeineCacheManager.registerCustomCache(CACHE_BROWSE_EXTENSION_FILES, browseCache);
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
            @Value("${ovsx.caching.average-review-rating.ttl:P3D}") Duration averageReviewRatingTtl,
            @Value("${ovsx.caching.namespace-details-json.ttl:PT1H}") Duration namespaceDetailsJsonTtl,
            @Value("${ovsx.caching.database-search.ttl:PT1H}") Duration databaseSearchTtl,
            @Value("${ovsx.caching.extension-json.ttl:PT1H}") Duration extensionJsonTtl,
            @Value("${ovsx.caching.latest-extension-version.ttl:PT1H}") Duration latestExtensionVersionTtl,
            @Value("${ovsx.caching.sitemap.ttl:PT1H}") Duration sitemapTtl,
            @Value("${ovsx.caching.malicious-extensions.ttl:P3D}") Duration maliciousExtensionsTtl
    ) {
        var extensionVersionMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration(
                        CACHE_AVERAGE_REVIEW_RATING,
                        redisCacheConfig(new GenericJackson2JsonRedisSerializer(), averageReviewRatingTtl)
                )
                .withCacheConfiguration(
                        CACHE_NAMESPACE_DETAILS_JSON,
                        redisCacheConfig(new Jackson2JsonRedisSerializer<>(NamespaceDetailsJson.class), namespaceDetailsJsonTtl)
                )
                .withCacheConfiguration(
                        CACHE_DATABASE_SEARCH,
                        redisCacheConfig(new Jackson2JsonRedisSerializer<>(SearchResult.class), databaseSearchTtl)
                )
                .withCacheConfiguration(
                        CACHE_EXTENSION_JSON,
                        redisCacheConfig(new Jackson2JsonRedisSerializer<>(ExtensionJson.class), extensionJsonTtl)
                )
                .withCacheConfiguration(
                        CACHE_LATEST_EXTENSION_VERSION,
                        redisCacheConfig(new Jackson2JsonRedisSerializer<>(extensionVersionMapper, ExtensionVersion.class), latestExtensionVersionTtl)
                )
                .withCacheConfiguration(
                        CACHE_SITEMAP,
                        redisCacheConfig(new StringRedisSerializer(), sitemapTtl)
                )
                .withCacheConfiguration(
                        CACHE_MALICIOUS_EXTENSIONS,
                        redisCacheConfig(new GenericJackson2JsonRedisSerializer(), maliciousExtensionsTtl)
                )
                .build();
    }

    private <T> RedisCacheConfiguration redisCacheConfig(RedisSerializer<T> serializer, Duration ttl) {
        var serializationPair = RedisSerializationContext.SerializationPair.fromSerializer(serializer);
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializationPair)
                .entryTtl(ttl);
    }
}
