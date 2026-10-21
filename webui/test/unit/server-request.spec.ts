/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { afterEach, describe, expect, it, vi } from 'vitest';
import { jsonResponse, stubFetch } from './support/fetch';
import { sendNonRetriableRequest, sendRequest, sendStrictRequest } from '../../src/server-request';

afterEach(() => {
    vi.unstubAllGlobals();
});

describe('sendStrictRequest', () => {
    it('resolves with the parsed body of a successful response', async () => {
        stubFetch(jsonResponse({ name: 'redhat' }));

        await expect(sendStrictRequest({ endpoint: 'https://open-vsx.org/api/redhat' })).resolves.toEqual({
            name: 'redhat'
        });
    });

    it('rejects with the error result when the server answers 200 with an error body', async () => {
        stubFetch(jsonResponse({ error: 'Namespace not found' }));

        await expect(sendStrictRequest({ endpoint: 'https://open-vsx.org/api/nope' })).rejects.toEqual({
            error: 'Namespace not found'
        });
    });

    // The registry aggregates per-version outcomes when deleting extension versions, so a fully
    // successful delete answers 200 with an empty error next to the success message. An empty error
    // is no error - rejecting it used to surface an error dialog with nothing in it.
    it('resolves a result whose success is reported alongside an empty error', async () => {
        stubFetch(jsonResponse({ success: 'Deleted 2 versions', error: '' }));

        await expect(
            sendStrictRequest({ endpoint: 'https://open-vsx.org/user/extension/foo/bar/delete', method: 'POST' })
        ).resolves.toEqual({ success: 'Deleted 2 versions', error: '' });
    });

    it('rejects with the error response of a failed request', async () => {
        stubFetch(jsonResponse({ error: 'Forbidden', message: 'no access' }, 403));

        await expect(sendStrictRequest({ endpoint: 'https://open-vsx.org/admin/namespace/x' })).rejects.toMatchObject({
            error: 'Forbidden',
            status: 403
        });
    });

    it('does not retry a server error - retries are owned by the query client', async () => {
        const fetchMock = stubFetch(jsonResponse({ error: 'boom' }, 500));

        await expect(sendStrictRequest({ endpoint: 'https://open-vsx.org/api/-/search' })).rejects.toMatchObject({
            status: 500
        });
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    // Regression test: sendRequest re-invokes itself on 429 without forwarding the original `retry`
    // flag, so it used to fall back to the default `retry = true` - re-enabling fetch-retry's own
    // retries for the follow-up request even though sendStrictRequest asked for none. A 500 after the
    // 429 would then be retried by fetch-retry itself instead of surfacing as a single rejection.
    it('does not re-enable retries for the request that follows a 429', async () => {
        vi.useFakeTimers();
        const fetchMock = stubFetch(jsonResponse({}, 429), jsonResponse({ error: 'boom' }, 500));

        // Attach the rejection assertion before letting the fake 429 delay elapse, so the rejection it
        // triggers is never briefly unhandled.
        const assertion = expect(
            sendStrictRequest({ endpoint: 'https://open-vsx.org/api/-/search' })
        ).rejects.toMatchObject({ status: 500 });
        await vi.runAllTimersAsync();
        await assertion;

        expect(fetchMock).toHaveBeenCalledTimes(2);
        vi.useRealTimers();
    });
});

describe('sendRequest', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    // A fresh Response per call: a body can only be read once.
    function stubFetchAlways(status: number) {
        const fetchMock = vi.fn((_input: RequestInfo | URL, init?: RequestInit) =>
            init?.signal?.aborted
                ? Promise.reject(new DOMException('The operation was aborted.', 'AbortError'))
                : Promise.resolve(jsonResponse({ error: 'boom' }, status))
        );
        vi.stubGlobal('fetch', fetchMock);
        return fetchMock;
    }

    // Regression test: a function `retryOn` replaces fetch-retry's `retries` cap, and ours never
    // checked `attempt`, so a persistent 5xx was retried forever.
    it('stops retrying a persistent server error after 10 retries', async () => {
        vi.useFakeTimers();
        const fetchMock = stubFetchAlways(500);

        const assertion = expect(sendRequest({ endpoint: 'https://open-vsx.org/api/-/search' })).rejects.toMatchObject({
            status: 500
        });
        await vi.runAllTimersAsync();
        await assertion;

        expect(fetchMock).toHaveBeenCalledTimes(11);
    });

    it('stops retrying once the request is aborted', async () => {
        vi.useFakeTimers();
        const fetchMock = stubFetchAlways(500);
        const abortController = new AbortController();

        const assertion = expect(
            sendRequest({ endpoint: 'https://open-vsx.org/api/-/search', abortController })
        ).rejects.toMatchObject({ name: 'AbortError' });
        await vi.advanceTimersByTimeAsync(0);
        abortController.abort();
        await vi.runAllTimersAsync();
        await assertion;

        // The retry already scheduled before the abort rejects and is not retried again.
        expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('gives up on a persistent 429 after 10 waits, even for a non-retriable request', async () => {
        vi.useFakeTimers();
        const fetchMock = stubFetchAlways(429);

        const assertion = expect(
            sendStrictRequest({ endpoint: 'https://open-vsx.org/api/-/search' })
        ).rejects.toMatchObject({ status: 429 });
        await vi.runAllTimersAsync();
        await assertion;

        expect(fetchMock).toHaveBeenCalledTimes(11);
    });

    it('settles as soon as the request is aborted while waiting out a 429', async () => {
        vi.useFakeTimers();
        const fetchMock = stubFetchAlways(429);
        const abortController = new AbortController();

        const assertion = expect(
            sendStrictRequest({ endpoint: 'https://open-vsx.org/api/-/search', abortController })
        ).rejects.toMatchObject({ name: 'AbortError' });
        await vi.advanceTimersByTimeAsync(0);
        abortController.abort();
        await assertion;

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(vi.getTimerCount()).toBe(0);
    });
});

describe('sendNonRetriableRequest', () => {
    it('resolves an error body instead of rejecting', async () => {
        stubFetch(jsonResponse({ error: 'Namespace not found' }));

        await expect(sendNonRetriableRequest({ endpoint: 'https://open-vsx.org/api/nope' })).resolves.toEqual({
            error: 'Namespace not found'
        });
    });

    // Regression test: `retry = false` used to hand fetch-retry an empty options object, whose
    // defaults still retried every fetch rejection (network errors, aborts) three times.
    it('does not retry a fetch rejection - retries are owned by the query client', async () => {
        vi.useFakeTimers();
        const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
        vi.stubGlobal('fetch', fetchMock);

        // Attach the rejection assertion before flushing timers so a retry's rejection is never unhandled.
        const assertion = expect(
            sendNonRetriableRequest({ endpoint: 'https://open-vsx.org/api/-/search' })
        ).rejects.toThrow('Failed to fetch');
        await vi.runAllTimersAsync();
        await assertion;

        expect(fetchMock).toHaveBeenCalledTimes(1);
        vi.useRealTimers();
    });
});
