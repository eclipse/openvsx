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
 * A setting is a public {@link WritableSetting} bean in a subpackage of its own. One the server
 * consults also exposes an accessor over {@link SettingsStore#snapshot()}, such as
 * {@link org.eclipse.openvsx.settings.readonly.ReadOnlySetting#isEnabled()}, and the rest of the
 * server injects the setting and asks that. A change made on another node arrives through
 * {@link SettingsUpdateChannel}, or once the snapshot expires after {@code ovsx.caching.setting.ttl}.
 * <p>
 * Adding a setting: the bean ({@link PublicSetting} too if visitors may see it), its fields on the
 * DTOs and their lines in {@link SettingsJsonMapper}.
 */
package org.eclipse.openvsx.settings;
