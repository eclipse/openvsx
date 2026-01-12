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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for extension scanning during publishing.
 * 
 * This controls the overall scan lifecycle and behavior.
 * Individual validation checks (like secret scanning, similarity) have their own configs.
 * 
 * Configuration example:
 * ovsx:
 *   scanning:
 *     enabled: true   # Enable extension scanning (default: false)
 */
@Configuration
public class ExtensionScanConfig {

    /**
     * When disabled, no scanning during publishing is performed.
     * Defaults to false - must be explicitly enabled in production.
     */
    @Value("${ovsx.scanning.enabled:false}")
    private boolean enabled;

    /**
     * Check if extension scanning is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
