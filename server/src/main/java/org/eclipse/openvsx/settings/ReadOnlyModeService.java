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

import org.springframework.stereotype.Service;

/**
 * Whether the registry is refusing writes right now: {@link ReadOnlySetting} as the server reads it.
 * Inject this to check the flag, never the setting, which only knows how to store it. Reads this
 * node's cached snapshot; see the package documentation for how a change made on another node
 * arrives.
 */
@Service
public class ReadOnlyModeService {

    private final SettingsService settings;
    private final ReadOnlySetting setting;

    public ReadOnlyModeService(SettingsService settings, ReadOnlySetting setting) {
        this.settings = settings;
        this.setting = setting;
    }

    /** {@code false} until an admin switches it on. */
    public boolean isEnabled() {
        return settings.current(setting);
    }
}
