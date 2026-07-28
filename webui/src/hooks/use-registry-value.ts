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

import { useContext } from 'react';
import { MainContext } from '../context';
import { RegistryVersion } from '../extension-registry-types';

/**
 * Derives a value from the registry version (from `MainContext`, populated from
 * `/api/version`) via `select`, so consumers can read server-provided config or
 * feature flags without touching the context. Returns `undefined` until the
 * version has loaded, or if it failed to load — so feature gates that treat that
 * as falsy fail closed, and other callers can supply their own fallback (`??`).
 */
export const useRegistryValue = <T>(select: (version: RegistryVersion) => T): T | undefined => {
    const { version } = useContext(MainContext);
    return version != null ? select(version) : undefined;
};
