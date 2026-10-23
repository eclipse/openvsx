/********************************************************************************
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
 ********************************************************************************/

import { describe, expect, it } from 'vitest';
import { controllerFromSignal, queryClient } from '../../src/query-client';

const { queries, mutations } = queryClient.getDefaultOptions();
const retry = queries?.retry as (failureCount: number, error: unknown) => boolean;
const retryDelay = queries?.retryDelay as (attempt: number, error: unknown) => number;

describe('queryClient retry policy', () => {
    it('retries a network error or a 5xx three times', () => {
        expect(retry(0, { status: 503 })).toBe(true);
        expect(retry(2, new TypeError('Failed to fetch'))).toBe(true);
        expect(retry(3, new TypeError('Failed to fetch'))).toBe(false);
    });

    it('does not retry a 4xx, or an error result the registry answered with', () => {
        expect(retry(0, { error: 'Forbidden', status: 403 })).toBe(false);
        expect(retry(0, { error: 'Not logged in.', status: 200 })).toBe(false);
    });

    it('waits out a 429 up to 10 times, for as long as the registry asks', () => {
        expect(retry(9, { status: 429 })).toBe(true);
        expect(retry(10, { status: 429 })).toBe(false);

        const delay = retryDelay(0, { status: 429, retryAfter: 5 });
        expect(delay).toBeGreaterThanOrEqual(6000);
        expect(delay).toBeLessThan(6100);
    });

    it('never retries a mutation, even a rate-limited one', () => {
        expect(mutations?.retry).toBeUndefined();
    });
});

describe('controllerFromSignal', () => {
    it('aborts the controller when the signal aborts', () => {
        const source = new AbortController();
        const controller = controllerFromSignal(source.signal);
        expect(controller.signal.aborted).toBe(false);

        source.abort();

        expect(controller.signal.aborted).toBe(true);
    });
});
