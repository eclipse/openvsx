/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link VSCodeGalleryExistenceCheckScanner}, extracted, to simplify testing.
 */
@Configuration
public class VSCodeGalleryExistenceCheckConfig {
    /**
     * Is NS verification check for upstream existing extensions enabled or not.
     * <p>
     * Property: {@code ovsx.scanning.gallery-existence-check.enabled}
     * Default: {@code false}
     */
    @Value("${ovsx.scanning.gallery-ownership.enabled:false}")
    private boolean enabled;

    /**
     * Is NS verification check for upstream existing extensions required or not.
     * <p>
     * Property: {@code ovsx.scanning.gallery-existence-check.required}
     * Default: {@code true}
     */
    @Value("${ovsx.scanning.gallery-ownership.required:true}")
    private boolean required;

    /**
     * Is NS verification check for upstream existing extensions enforced or not.
     * <p>
     * Property: {@code ovsx.scanning.gallery-existence-check.enforced}
     * Default: {@code true}
     */
    @Value("${ovsx.scanning.gallery-ownership.enforced:true}")
    private boolean enforced;

    /**
     * The upstream gallery API URL to perform the existence checks against.
     * <p>
     * Property: {@code ovsx.scanning.gallery-existence-check.gallery-url}
     * Default: {@code ""}
     */
    @Value("${ovsx.scanning.gallery-ownership.gallery-url:}")
    private String galleryUrl;

    /**
     * Default constructor.
     */
    public VSCodeGalleryExistenceCheckConfig() {
    }

    /**
     * For testing.
     */
    public VSCodeGalleryExistenceCheckConfig(boolean enabled, boolean required, boolean enforced, String galleryUrl) {
        this.enabled = enabled;
        this.required = required;
        this.enforced = enforced;
        this.galleryUrl = galleryUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isEnforced() {
        return enforced;
    }

    public String getGalleryUrl() {
        return galleryUrl;
    }

    @PostConstruct
    public void validate() {
        if (enabled) {
            if (galleryUrl == null || galleryUrl.isEmpty()) {
                throw new IllegalStateException("ovsx.scanning.gallery-ownership.gallery-url must be set");
            }
        }
    }
}
