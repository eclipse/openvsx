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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadAnalyticsVisibilityServiceTest {

    private final SettingsService settings = mock(SettingsService.class);
    private final DownloadAnalyticsSetting setting = new DownloadAnalyticsSetting(true);

    @Test
    void followsTheSwitchWhereAnalyticsIsConfigured() {
        when(settings.current(setting)).thenReturn(true);

        var visibility = new DownloadAnalyticsVisibilityService(settings, setting, true);

        assertThat(visibility.isVisible()).isTrue();
    }

    @Test
    void staysOffWhereAnalyticsIsNotConfiguredWhateverTheSwitch() {
        when(settings.current(setting)).thenReturn(true);

        var visibility = new DownloadAnalyticsVisibilityService(settings, setting, false);

        assertThat(visibility.isVisible()).isFalse();
    }
}
