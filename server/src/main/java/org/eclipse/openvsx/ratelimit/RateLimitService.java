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

import com.giffing.bucket4j.spring.boot.starter.config.cache.CacheUpdateEvent;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.eclipse.openvsx.cache.JedisClusterCacheManager;
import org.eclipse.openvsx.entities.RefillStrategy;
import org.eclipse.openvsx.ratelimit.config.TieredRateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.eclipse.openvsx.ratelimit.config.TieredRateLimitConfig.CACHE_RATE_LIMIT_BUCKET;

@Component
@ConditionalOnBean(TieredRateLimitConfig.class)
public class RateLimitService {
    private final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final ProxyManager<byte[]> proxyManager;
    private final JedisClusterCacheManager<String, String> cacheManager;
    private final CacheManager rateLimitCacheManager;

    public RateLimitService(
            ProxyManager<byte[]> proxyManager,
            JedisClusterCacheManager<String, String> cacheManager,
            @Qualifier("rateLimitCacheManager") CacheManager rateLimitCacheManager) {
        this.proxyManager = proxyManager;
        this.cacheManager = cacheManager;
        this.rateLimitCacheManager = rateLimitCacheManager;
    }

    @Cacheable(value = CACHE_RATE_LIMIT_BUCKET, cacheManager = "rateLimitCacheManager")
    public Bucket getBucket(ResolvedIdentity identity) {
        var bandwidth = getBandwidth(identity);
        var bucketConfiguration =
                BucketConfiguration.builder()
                        .addLimit(bandwidth)
                        .build();

        var bucket = proxyManager.builder().build(identity.cacheKey().getBytes(StandardCharsets.UTF_8), () -> bucketConfiguration);
        bucket.replaceConfiguration(bucketConfiguration, TokensInheritanceStrategy.RESET);
        return bucket;
    }

    private Bandwidth getBandwidth(ResolvedIdentity identity) {
        if (identity.isCustomer()) {
            var tier = identity.getCustomer().getTier();

            logger.info("getting bandwidth for customer {} - {}", identity.getCustomer().getName(), tier.getCapacity());

            var fillStage = Bandwidth.builder().capacity(tier.getCapacity());

            var buildStage = switch (tier.getRefillStrategy()) {
                case RefillStrategy.GREEDY -> fillStage.refillGreedy(tier.getCapacity(), tier.getDuration());
                case RefillStrategy.INTERVAL -> fillStage.refillIntervally(tier.getCapacity(), tier.getDuration());
            };

            return buildStage.build();
        } else {
            // TODO: get data for free tier from db
            return Bandwidth.builder().capacity(10000).refillGreedy(10000, Duration.ofMinutes(5)).build();
        }
    }

    @CacheEvict(value = CACHE_RATE_LIMIT_BUCKET, cacheManager = "rateLimitCacheManager", allEntries = true)
    public void evictBucketCache() {
        logger.info("evict bucket cache");
    }

    public void evictConfigurationCache() {
        cacheManager.setValue("test", "1");
    }

    @EventListener(CacheUpdateEvent.class)
    public void onCacheUpdateEvent(CacheUpdateEvent<String, String> event) {
        evictBucketCache();
    }
}
