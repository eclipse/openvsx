/******************************************************************************
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
 *****************************************************************************/
package org.eclipse.openvsx.settings;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.openvsx.cache.jedis.JedisClusterChannelListener;
import org.eclipse.openvsx.json.SettingsJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;

@Service
public class SettingsService {

    private static final String SETTINGS_UPDATE_CHANNEL = "settings";

    private final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final @Nullable JedisCluster jedisCluster;
    private final SettingsUpdateListener settingsUpdateListener;

    private boolean readOnlyMode = false;

    public SettingsService(@Nullable JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;

        if (jedisCluster != null) {
            settingsUpdateListener = new SettingsUpdateListener(jedisCluster);
            logger.info("SettingsService initialized with Redis update listener");
        } else {
            settingsUpdateListener = null;
        }
    }

    private void publishSettingsUpdate() {
        if (jedisCluster != null) {
            logger.debug("Publish settings update");
            String version = String.valueOf(System.currentTimeMillis());
            jedisCluster.publish(SETTINGS_UPDATE_CHANNEL, version);
        }
    }

    @PostConstruct
    public void initialize() {
        if (settingsUpdateListener != null) {
            settingsUpdateListener.startSubscriber();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (settingsUpdateListener != null) {
            settingsUpdateListener.shutdown();
        }
    }

    public boolean isReadOnly() {
        return readOnlyMode;
    }

    public SettingsJson getCurrent() {
        var json = new SettingsJson();
        json.setReadOnly(isReadOnly());
        return json;
    }

    public void updateFromJson(SettingsJson newSettings) {
        readOnlyMode = newSettings.isReadOnly();
        publishSettingsUpdate();
    }

    private class SettingsUpdateListener extends JedisClusterChannelListener {
        SettingsUpdateListener(JedisCluster jedisCluster) {
            super(jedisCluster, SETTINGS_UPDATE_CHANNEL, "SettingsUpdate");
        }

        @Override
        public void onMessage(String channel, String message) {
            if (SETTINGS_UPDATE_CHANNEL.equals(channel)) {
                logger.info("received settings update");
            }
        }
    }
}
