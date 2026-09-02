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

import { vi } from 'vitest';

/** A JSON response as the registry answers it; `status` defaults to 200. */
export function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' }
    });
}

/**
 * Stubs the global `fetch` to answer with `responses` in call order. Undo it with
 * `vi.unstubAllGlobals()`. Returns the mock so a spec can assert on the calls.
 */
export function stubFetch(...responses: Response[]) {
    const fetchMock = vi.fn();
    responses.forEach(response => fetchMock.mockResolvedValueOnce(response));
    vi.stubGlobal('fetch', fetchMock);
    return fetchMock;
}
