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

import { QueryClient } from '@tanstack/react-query';

// Shared singleton query client for the whole app. Created once at module scope
// so the cache survives re-renders. Defaults are intentionally conservative to
// match the previous fetch-on-mount behaviour as closely as possible:
//  - refetchOnWindowFocus is disabled (the UI never refetched on focus before),
//  - staleTime gives a short window where cached data is reused without refetching,
//  - retry mirrors a single retry rather than the library default of three.
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            staleTime: 60 * 1000,
            retry: 1,
        },
    },
});

/**
 * Bridge between TanStack Query's `AbortSignal` and the `AbortController` that
 * `ExtensionRegistryService` methods expect. Inside a `queryFn` we get a
 * `signal`; this wraps it in a controller that aborts when the signal does, so
 * we can keep the service signatures untouched while dropping component-level
 * `AbortController` refs.
 */
export function controllerFromSignal(signal?: AbortSignal): AbortController {
    const controller = new AbortController();
    if (signal) {
        if (signal.aborted) {
            controller.abort();
        } else {
            signal.addEventListener('abort', () => controller.abort(), { once: true });
        }
    }
    return controller;
}
