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

/**
 * Runtime settings: values an admin changes while the server runs, stored as rows and cached on
 * each node.
 * <p>
 * A setting is a package-private {@link WritableSetting} bean. It only ever sees rows handed to it
 * and never reads the store. If the server consults the value, a public reader such as
 * {@link ReadOnlyMode} asks {@link SettingsService#current} for it behind a name that says what the
 * value means; the reader is a bean of its own precisely because the setting must not know the
 * store. The rest of the server injects readers and nothing else: {@link SettingsService} is the
 * only way to a value, and {@link SettingsCache} stays package-private behind it.
 * <p>
 * A reader sees this node's snapshot. A change made on another node arrives through
 * {@link SettingsUpdateChannel}, or once the snapshot expires after {@code ovsx.caching.setting.ttl}.
 * <p>
 * Adding a setting: the bean here ({@link PublicSetting} too if visitors may see it), a reader if
 * the server consults it, its fields on the DTOs and their lines in {@link SettingsJsonMapper}.
 */
package org.eclipse.openvsx.settings;
