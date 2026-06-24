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
import type { ErrorResponse } from './server-request';

// Shared singleton query client for the whole app. Created once at module scope
// so the cache survives re-renders. Defaults are intentionally conservative to
// match the previous fetch-on-mount behaviour as closely as possible:
//  - refetchOnWindowFocus is disabled (the UI never refetched on focus before),
//  - staleTime gives a short window where cached data is reused without refetching.
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            staleTime: 60 * 1000,
            // Retries are now owned here: migrated requests go through `sendNonRetriableRequest`,
            // so the fetch-retry loop no longer runs for them. This mirrors the old
            // fetch-retry policy — retry transient failures (network errors and 5xx) with
            // exponential backoff, but never retry client errors (4xx), which won't recover.
            // (429s never reach here: `sendRequest` waits out the rate limit internally.)
            retry: (failureCount, error) => {
                const status = (error as Partial<ErrorResponse> | null)?.status;
                if (status !== undefined && status < 500) {
                    return false;
                }
                return failureCount < 3;
            },
            retryDelay: attempt => {
                const backoff = Math.min(1000 * 2 ** attempt, 30000);
                return Math.round(backoff / 2 + Math.random() * (backoff / 2));
            }
        }
    }
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
