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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giffing.bucket4j.spring.boot.starter.config.cache.CacheManager;
import com.giffing.bucket4j.spring.boot.starter.config.cache.CacheUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;

public class JedisClusterCacheManager<K, V> implements CacheManager<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JedisClusterCacheManager.class);

    private final JedisCluster cluster;
    private final String cacheName;
    private final Class<V> valueType;
    private final ObjectMapper objectMapper;
    private final String updateChannel;

    /**
     * @param cluster The JedisCluster to use for reading/writing data to the cache
     * @param cacheName The name of the cache.
     * @param valueType The type of the data. This is required for parsing and should always match the V of this class.
     */
    public JedisClusterCacheManager(JedisCluster cluster, String cacheName, Class<V> valueType) {
        this.cluster = cluster;
        this.cacheName = cacheName;
        this.valueType = valueType;

        this.objectMapper = new ObjectMapper();
        this.updateChannel = cacheName.concat(":update");
    }


    @Override
    public V getValue(K key) {
        try {
            String serializedValue = cluster.hget(cacheName, objectMapper.writeValueAsString(key));
            return serializedValue != null ? objectMapper.readValue(serializedValue, this.valueType) : null;
        } catch (JsonProcessingException e) {
            LOGGER.warn("Exception occurred while retrieving key '{}' from cache '{}'. Message: {}", key, cacheName, e.getMessage());
            return null;
        }
    }

    @Override
    public void setValue(K key, V value) {
        try {
            V oldValue = getValue(key);

            String serializedKey = objectMapper.writeValueAsString(key);
            String serializedValue = objectMapper.writeValueAsString(value);
            cluster.hset(this.cacheName, serializedKey, serializedValue);

            //publish an update event if the key already existed
            if(oldValue != null){
                CacheUpdateEvent<K,V> updateEvent = new CacheUpdateEvent<>(key, oldValue, value);
                cluster.publish(this.updateChannel, objectMapper.writeValueAsString(updateEvent));
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Exception occurred while setting key '{}' in cache '{}'. Message: {}", key, cacheName, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

