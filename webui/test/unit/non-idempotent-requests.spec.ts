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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { jsonResponse } from './support/fetch';
import { ExtensionRegistryService } from '../../src/extension-registry-service';
import { Extension, NewReview, PersonalAccessToken, UserData } from '../../src/extension-registry-types';

const serverUrl = 'https://registry.test';
const csrfUrl = `${serverUrl}/user/csrf`;
const user = {
    loginName: 'alice',
    provider: 'github',
    createTokenUrl: `${serverUrl}/user/token/create`
} as UserData;
const extension = { namespace: 'foo', name: 'bar' } as Extension;
const token = { deleteTokenUrl: `${serverUrl}/user/token/delete/1` } as PersonalAccessToken;

type Call = (service: ExtensionRegistryService, abortController: AbortController) => Promise<unknown>;

// A 502 can arrive after the server has committed the change, so resending would repeat it.
const cases: [string, Call][] = [
    ['postReview', (s, ac) => s.postReview(ac, { rating: 5 } as NewReview, `${serverUrl}/review`)],
    ['deleteReview', (s, ac) => s.deleteReview(ac, `${serverUrl}/review/delete`)],
    ['deleteUserReview', (s, ac) => s.deleteUserReview(ac, extension, user)],
    ['createAccessToken', (s, ac) => s.createAccessToken(ac, user, 'ci')],
    ['deleteAccessToken', (s, ac) => s.deleteAccessToken(ac, token)],
    ['deleteAllAccessTokens', (s, ac) => s.deleteAllAccessTokens(ac, [token])],
    ['setNamespaceMember', (s, ac) => s.setNamespaceMember(ac, `${serverUrl}/members`, user, 'contributor')],
    ['signPublisherAgreement', (s, ac) => s.signPublisherAgreement(ac)]
];

describe('non-idempotent requests', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it.each(cases)('%s is not resent after a 502', async (_name, call) => {
        const fetchMock = vi.fn((input: RequestInfo | URL) =>
            Promise.resolve(
                String(input) === csrfUrl
                    ? jsonResponse({ header: 'X-XSRF-TOKEN', value: 'token' })
                    : jsonResponse({ error: 'Bad Gateway' }, 502)
            )
        );
        vi.stubGlobal('fetch', fetchMock);

        const result = expect(
            call(new ExtensionRegistryService(serverUrl), new AbortController())
        ).rejects.toMatchObject({ status: 502 });
        await vi.runAllTimersAsync();
        await result;

        expect(fetchMock.mock.calls.filter(([input]) => String(input) !== csrfUrl)).toHaveLength(1);
    });
});
