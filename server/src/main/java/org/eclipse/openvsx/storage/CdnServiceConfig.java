/********************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Supports configuring a CDN front URL for different storage providers.
 * <p>
 * If a CDN front URL is configured for a specific storage type, the URL locations
 * for file resources and namespace logos in {@link StorageUtilService} are returned as
 * {@code frontURL + "/" + objectKey(resource)}
 * <p>
 * Example config for AWS:
 * <pre>
 * ovsx:
 *   storage:
 *     cdn:
 *       enabled: true
 *       services:
 *         - aws: https://my.cdn.front.domain/
 * </pre>
 */
@Configuration
@ConfigurationProperties("ovsx.storage.cdn")
public class CdnServiceConfig {
    private boolean enabled;
    private Map<String, String> services;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }

    public @Nullable String getCdnFrontUrl(String storageType) {
        return enabled ? services.get(storageType) : null;
    }
}
