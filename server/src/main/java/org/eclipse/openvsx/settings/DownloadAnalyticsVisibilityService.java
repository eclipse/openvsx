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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Whether visitors get download analytics right now: {@link DownloadAnalyticsSetting} as the server
 * reads it, on a registry that has download analytics configured at all. Inject this to check the
 * switch, never the setting, which only knows how to store it. Reads this node's cached snapshot;
 * see the package documentation for how a change made on another node arrives.
 */
@Service
public class DownloadAnalyticsVisibilityService {

    private final SettingsService settings;
    private final DownloadAnalyticsSetting setting;
    private final boolean analyticsConfigured;

    public DownloadAnalyticsVisibilityService(
            SettingsService settings,
            DownloadAnalyticsSetting setting,
            @Value("${ovsx.analytics.enabled:false}") boolean analyticsConfigured
    ) {
        this.settings = settings;
        this.setting = setting;
        this.analyticsConfigured = analyticsConfigured;
    }

    /** {@code false} until an admin switches it on, and always where analytics is not configured. */
    public boolean isVisible() {
        return analyticsConfigured && settings.current(setting);
    }
}
