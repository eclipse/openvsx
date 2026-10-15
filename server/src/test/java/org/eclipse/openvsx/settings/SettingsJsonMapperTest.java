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

import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.eclipse.openvsx.json.SettingsJson;
import org.eclipse.openvsx.publish.PublishingConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsJsonMapperTest {

    private final PublishingConfig publishingConfig = mock(PublishingConfig.class);
    private final SettingsJsonMapper mapper = new SettingsJsonMapper(publishingConfig);

    @Test
    void theRowsUseTheStoredKebabCaseKeys() {
        var json = new SettingsJson();
        json.setReadOnly(true);
        json.setBannerMessage("Maintenance tonight");
        json.setDownloadAnalyticsEnabled(false);

        assertThat(mapper.toRows(json).rows())
                .containsExactly(
                        entry(ReadOnlySetting.KEY, true),
                        entry(BannerSetting.KEY_MESSAGE, "Maintenance tonight"),
                        entry(DownloadAnalyticsSetting.KEY, false));
    }

    @Test
    void aSettingLeftOutOfTheRequestProducesNoRow() {
        assertThat(mapper.toRows(new SettingsJson()).rows()).isEmpty();
    }

    @Test
    void rowsReadBackIntoThePayloadWithTheCeiling() {
        when(publishingConfig.getMaxOverrideSize()).thenReturn(1024L);
        var json = mapper.toJson(
                new SettingRows(
                        Map.of(
                                ReadOnlySetting.KEY,
                                true,
                                BannerSetting.KEY_SEVERITY,
                                "warning",
                                BannerSetting.KEY_MESSAGE,
                                "Hi",
                                DownloadAnalyticsSetting.KEY,
                                true)));

        assertThat(json.getReadOnly()).isTrue();
        assertThat(json.getBannerSeverity()).isEqualTo("warning");
        assertThat(json.getBannerMessage()).isEqualTo("Hi");
        assertThat(json.getBannerEnabled()).isNull();
        assertThat(json.getDownloadAnalyticsEnabled()).isTrue();
        assertThat(json.getMaxOverrideSize()).isEqualTo(1024L);
    }

    @Test
    void theSiteViewLeavesOutTheAdminOnlyRows() {
        var json = mapper
                .toSiteJson(new SettingRows(Map.of(ReadOnlySetting.KEY, true, MaxExtensionSizeSetting.KEY, 1L)));

        var jackson = JsonMapper.shared();
        assertThat(jackson.readTree(jackson.writeValueAsString(json)).propertyNames()).isEmpty();
    }

    @Test
    void theSiteViewCarriesTheDownloadAnalyticsSwitch() {
        var json = mapper.toSiteJson(new SettingRows(Map.of(DownloadAnalyticsSetting.KEY, false)));

        assertThat(json.getDownloadAnalyticsEnabled()).isFalse();
    }
}
