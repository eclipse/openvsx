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
import io.micrometer.common.util.StringUtils;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.event.EventType;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
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

import javax.cache.Caching;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.cache.CacheService.*;

@Configuration
@EnableCaching(proxyTargetClass = true)
public class CacheConfig {

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
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        var serializationPair = RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());
        var defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializationPair);

        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration(CACHE_AVERAGE_REVIEW_RATING, defaultCacheConfig.entryTtl(Duration.ofDays(1000)))
                .withCacheConfiguration(CACHE_NAMESPACE_DETAILS_JSON, defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_DATABASE_SEARCH, defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_EXTENSION_JSON, defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_LATEST_EXTENSION_VERSION, defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("buckets", defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_SITEMAP, defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_MALICIOUS_EXTENSIONS, defaultCacheConfig.entryTtl(Duration.ofDays(1)))
                .build();
    }

    @Bean
    public CacheManager ehcacheManager() {
        var provider = Caching.getCachingProvider();
        var ehcacheProvider = (EhcacheCachingProvider) provider;

        var configuration = new DefaultConfiguration(
                ehcacheProvider.getDefaultClassLoader(),
                new DefaultPersistenceConfiguration(new File(System.getProperty("java.io.tmpdir")))
        );

        var cacheManager = ehcacheProvider.getCacheManager(ehcacheProvider.getDefaultURI(), configuration);

        var cacheEventListenerConfiguration = CacheEventListenerConfigurationBuilder
                .newEventListenerConfiguration(new ExpiredFileListener(), EventType.EXPIRED, EventType.EVICTED, EventType.REMOVED, EventType.UPDATED)
                .unordered()
                .asynchronous();

        var webResourceCacheConfigBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                String.class,
                                Path.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(150, EntryUnit.ENTRIES))
                .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofHours(1)))
                .withService(cacheEventListenerConfiguration);

        var webResourceCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(webResourceCacheConfigBuilder);
        cacheManager.createCache(CACHE_WEB_RESOURCE_FILES, webResourceCacheConfig);

        var extensionCacheConfigBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class,
                        Path.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(20, EntryUnit.ENTRIES))
                .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofHours(1)))
                .withService(cacheEventListenerConfiguration);

        var extensionCacheConfig = Eh107Configuration.fromEhcacheCacheConfiguration(extensionCacheConfigBuilder);
        cacheManager.createCache(CACHE_EXTENSION_FILES, extensionCacheConfig);
        return new JCacheCacheManager(cacheManager);
    }
}
