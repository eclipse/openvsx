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
import { sendNonRetriableRequest, sendStrictRequest } from '../../src/server-request';

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' }
    });
}

function stubFetch(...responses: Response[]) {
    const fetchMock = vi.fn();
    responses.forEach(response => fetchMock.mockResolvedValueOnce(response));
    vi.stubGlobal('fetch', fetchMock);
    return fetchMock;
}

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

describe('sendNonRetriableRequest', () => {
    it('resolves an error body instead of rejecting', async () => {
        stubFetch(jsonResponse({ error: 'Namespace not found' }));

        await expect(sendNonRetriableRequest({ endpoint: 'https://open-vsx.org/api/nope' })).resolves.toEqual({
            error: 'Namespace not found'
        });
    });
});
