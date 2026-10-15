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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.eclipse.openvsx.util.AfterCommitExecutor;

/**
 * Serves the registry's settings to the two endpoints that read them and applies an admin's update
 * across every registered {@link WritableSetting}. It knows what a setting is, never what any
 * particular setting means: names, row keys, legal values, and defaults all belong to
 * the bean. Code that needs a setting's value at runtime injects that setting's reader, see the
 * package documentation, not this.
 */
@Service
public class SettingsService {

    private final List<WritableSetting<?>> settings;
    private final SettingsCache cache;
    private final SettingsUpdateChannel channel;
    private final AfterCommitExecutor afterCommit;
    private final ApplicationEventPublisher events;

    /**
     * Settings are ordered by name so the audit line and the public settings object do not reshuffle
     * with bean discovery order.
     */
    public SettingsService(
            List<WritableSetting<?>> settings,
            SettingsCache cache,
            SettingsUpdateChannel channel,
            AfterCommitExecutor afterCommit,
            ApplicationEventPublisher events
    ) {
        this.settings = settings.stream().sorted(Comparator.comparing(WritableSetting::getName)).toList();
        this.cache = cache;
        this.channel = channel;
        this.afterCommit = afterCommit;
        this.events = events;
    }

    /** Every setting's rows with its defaults applied, drafts included. The admin view. */
    public SettingRows getCurrentSettings() {
        var stored = cache.snapshot();
        var rows = new LinkedHashMap<String, Object>();
        settings.forEach(setting -> rows.putAll(currentRows(setting, stored)));
        return new SettingRows(rows);
    }

    /** Only what the settings implementing {@link PublicSetting} choose to publish. */
    public SettingRows getSiteSettings() {
        var stored = cache.snapshot();
        var rows = new LinkedHashMap<String, Object>();
        for (var setting : settings) {
            if (setting instanceof PublicSetting published) {
                rows.putAll(published.publicView(stored));
            }
        }
        return new SettingRows(rows);
    }

    /**
     * One setting's value as this node sees it, from the cached snapshot. For the readers in this
     * package that the server consults at runtime, hence not public; a save reads the store itself.
     */
    <T> T current(WritableSetting<T> setting) {
        return setting.read(cache.snapshot());
    }

    /**
     * Drops every cached setting on this node and tells the others to. For callers that change data
     * a setting derives from, such as the size overrides behind the ceiling: evicting one key is not
     * enough. Deferred to after the commit, or a node could refill the cache from the rows as they
     * still are.
     */
    public void invalidateCache() {
        afterCommit.execute(() -> {
            cache.clear();
            channel.publish();
            events.publishEvent(new SettingsChangedEvent());
        });
    }

    /**
     * Applies an admin's update and returns the audit line. Every setting is merged and validated
     * before anything is written, so a refusal leaves the store untouched; the rows then go in as one
     * transaction.
     */
    public String update(SettingRows update) {
        var stored = cache.load();

        var pending = new LinkedHashMap<String, Object>();
        var described = new ArrayList<String>();
        for (var setting : settings) {
            changedRows(setting, stored, update).forEach((rowKey, value) -> {
                pending.put(rowKey, value);
                described.add(setting.describe(rowKey, value));
            });
        }
        if (pending.isEmpty()) {
            return "";
        }

        cache.setAll(pending);
        cache.clear();
        channel.publish();
        afterCommit.execute(() -> events.publishEvent(new SettingsChangedEvent()));
        return String.join(", ", described);
    }

    /** Merges, validates and returns the rows this setting wants written. */
    private static <T> Map<String, Object> changedRows(
            WritableSetting<T> setting,
            SettingRows stored,
            SettingRows update
    ) {
        var current = setting.read(stored);
        var merged = setting.merge(current, update);
        setting.validate(merged, current);

        var currentRows = setting.toRows(current);
        var changed = new LinkedHashMap<String, Object>(setting.toRows(merged));
        // A row the update carries is written even when it equals the current value: that value
        // comes from a node-local snapshot that can be a minute stale, so skipping the write could
        // drop the very change the admin asked for.
        changed.entrySet()
                .removeIf(
                        row -> !update.rows().containsKey(row.getKey())
                                && Objects.equals(row.getValue(), currentRows.get(row.getKey())));
        return changed;
    }

    private static <T> Map<String, Object> currentRows(WritableSetting<T> setting, SettingRows stored) {
        return setting.toRows(setting.read(stored));
    }
}
