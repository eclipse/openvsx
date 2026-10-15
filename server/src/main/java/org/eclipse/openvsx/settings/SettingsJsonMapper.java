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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.json.SettingsJson;
import org.eclipse.openvsx.json.SiteSettingsJson;
import org.eclipse.openvsx.publish.PublishingConfig;

/**
 * The one place that turns stored rows into the wire DTOs and back, and so the one class that spells
 * every setting's row keys; the DTOs themselves name no setting. Properties are camelCase like every
 * other DTO, rows kebab-case as stored.
 */
@Component
public class SettingsJsonMapper {

    private final PublishingConfig publishingConfig;

    public SettingsJsonMapper(PublishingConfig publishingConfig) {
        this.publishingConfig = publishingConfig;
    }

    /**
     * The admin view: every row the settings report, plus the ceiling no size can be raised past,
     * which is configuration rather than a setting. A field no row backs stays null, and so off the
     * wire.
     */
    public SettingsJson toJson(SettingRows stored) {
        var rows = stored.rows();
        var json = new SettingsJson();
        json.setReadOnly(bool(rows, ReadOnlySetting.KEY));
        json.setMaxExtensionSize(number(rows, MaxExtensionSizeSetting.KEY));
        json.setMaxOverrideSize(publishingConfig.getMaxOverrideSize());
        json.setBannerEnabled(bool(rows, BannerSetting.KEY_ENABLED));
        json.setBannerMessage(string(rows, BannerSetting.KEY_MESSAGE));
        json.setBannerSeverity(string(rows, BannerSetting.KEY_SEVERITY));
        json.setBannerDismissId(string(rows, BannerSetting.KEY_DISMISS_ID));
        json.setDownloadAnalyticsEnabled(bool(rows, DownloadAnalyticsSetting.KEY));
        return json;
    }

    /** The rows an update carries. A null field is a setting the caller isn't touching, so it produces no row. */
    public SettingRows toRows(SettingsJson json) {
        var rows = new LinkedHashMap<String, Object>();
        putIfPresent(rows, ReadOnlySetting.KEY, json.getReadOnly());
        putIfPresent(rows, MaxExtensionSizeSetting.KEY, json.getMaxExtensionSize());
        putIfPresent(rows, BannerSetting.KEY_ENABLED, json.getBannerEnabled());
        putIfPresent(rows, BannerSetting.KEY_MESSAGE, json.getBannerMessage());
        putIfPresent(rows, BannerSetting.KEY_SEVERITY, json.getBannerSeverity());
        putIfPresent(rows, BannerSetting.KEY_DISMISS_ID, json.getBannerDismissId());
        putIfPresent(rows, DownloadAnalyticsSetting.KEY, json.getDownloadAnalyticsEnabled());
        return new SettingRows(rows);
    }

    /** The published rows; a row with no property on the site DTO is left out. */
    public SiteSettingsJson toSiteJson(SettingRows published) {
        var rows = published.rows();
        var json = new SiteSettingsJson();
        json.setBannerEnabled(bool(rows, BannerSetting.KEY_ENABLED));
        json.setBannerMessage(string(rows, BannerSetting.KEY_MESSAGE));
        json.setBannerSeverity(string(rows, BannerSetting.KEY_SEVERITY));
        json.setBannerDismissId(string(rows, BannerSetting.KEY_DISMISS_ID));
        json.setDownloadAnalyticsEnabled(bool(rows, DownloadAnalyticsSetting.KEY));
        return json;
    }

    private static void putIfPresent(Map<String, Object> rows, String key, @Nullable Object value) {
        if (value != null) {
            rows.put(key, value);
        }
    }

    private static @Nullable Boolean bool(Map<String, Object> rows, String key) {
        return rows.get(key) instanceof Boolean value ? value : null;
    }

    private static @Nullable Long number(Map<String, Object> rows, String key) {
        return rows.get(key) instanceof Number value ? value.longValue() : null;
    }

    private static @Nullable String string(Map<String, Object> rows, String key) {
        return rows.get(key) instanceof String value ? value : null;
    }
}
