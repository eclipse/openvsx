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

import * as http from 'http';
import { AddressInfo } from 'net';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Registry } from '../../src/registry';
import { getTrustedPublishingToken, refreshTrustedPublishingToken } from '../../src/trusted-publishing';

interface FakeRegistry {
    url: string;
    exchanges: number;
}

// Answers the token exchange with the given statuses in order, the last one repeating.
async function startRegistry(...responses: Array<{ status: number; body: unknown }>): Promise<FakeRegistry> {
    const state: FakeRegistry = { url: '', exchanges: 0 };
    const server = http.createServer((req, res) => {
        const response = responses[Math.min(state.exchanges, responses.length - 1)];
        state.exchanges++;
        req.resume();
        req.on('end', () => {
            res.writeHead(response.status, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(response.body));
        });
    });
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    state.url = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    servers.push(server);
    return state;
}

const servers: http.Server[] = [];

describe('trusted publishing token exchange', () => {
    beforeEach(() => {
        servers.length = 0;
    });

    afterEach(async () => {
        await Promise.all(servers.map(server => new Promise(resolve => server.close(resolve))));
    });

    // The registry answers 503 when it could not reach the identity provider, which says nothing about
    // the token. Failing a release build on a blip reaching GitHub would be the wrong call.
    it('retries the exchange when the registry could not verify the token', async () => {
        const registry = await startRegistry(
            { status: 503, body: { error: 'Could not verify the token with GitHub, please retry.' } },
            { status: 201, body: { value: 'the-access-token' } }
        );

        const token = await getTrustedPublishingToken(
            new Registry({ registryUrl: registry.url }),
            'retried',
            'ext',
            { idToken: 'an-id-token' }
        );

        expect(token).toBe('the-access-token');
        expect(registry.exchanges).toBe(2);
    });

    // A refusal is final: no registration matches, so trying again cannot help.
    it('does not retry when the registry refuses the token', async () => {
        const registry = await startRegistry({
            status: 403,
            body: { error: 'No trusted publisher matches the presented token.' }
        });

        await expect(
            getTrustedPublishingToken(new Registry({ registryUrl: registry.url }), 'refused', 'ext', {
                idToken: 'an-id-token'
            })
        ).rejects.toThrow('No trusted publisher matches the presented token.');

        expect(registry.exchanges).toBe(1);
    });

    // The issued token is short-lived and shared by every target platform of a release, so a slow
    // fan-out can outlive it. Refusing it must be answerable with a new one rather than failing a
    // release that was authorised.
    it('exchanges again when the token it was publishing with is refused', async () => {
        const registry = await startRegistry(
            { status: 201, body: { value: 'first-token' } },
            { status: 201, body: { value: 'second-token' } }
        );
        const options = { idToken: 'an-id-token' };
        const client = new Registry({ registryUrl: registry.url });

        const first = await getTrustedPublishingToken(client, 'expiring', 'ext', options);
        const second = await refreshTrustedPublishingToken(client, 'expiring', 'ext', options, first);

        expect(first).toBe('first-token');
        expect(second).toBe('second-token');
        expect(registry.exchanges).toBe(2);
        // and the replacement is what every later target platform picks up
        expect(await getTrustedPublishingToken(client, 'expiring', 'ext', options)).toBe('second-token');
    });

    // Target platforms publish concurrently, so they hit one expiry together. Exchanging per refusal
    // would ask the identity provider once per target and hand out tokens the others discard.
    it('exchanges once when every target platform is refused at the same time', async () => {
        const registry = await startRegistry(
            { status: 201, body: { value: 'first-token' } },
            { status: 201, body: { value: 'second-token' } }
        );
        const options = { idToken: 'an-id-token' };
        const client = new Registry({ registryUrl: registry.url });

        const stale = await getTrustedPublishingToken(client, 'concurrent', 'ext', options);
        const refreshed = await Promise.all(
            [0, 1, 2, 3].map(() =>
                refreshTrustedPublishingToken(client, 'concurrent', 'ext', options, stale))
        );

        expect(refreshed).toEqual(['second-token', 'second-token', 'second-token', 'second-token']);
        expect(registry.exchanges).toBe(2);
    });

    // A token already replaced is not stale twice: the caller that lost the race takes what is cached.
    it('does not exchange again for a token that was already replaced', async () => {
        const registry = await startRegistry(
            { status: 201, body: { value: 'first-token' } },
            { status: 201, body: { value: 'second-token' } },
            { status: 201, body: { value: 'third-token' } }
        );
        const options = { idToken: 'an-id-token' };
        const client = new Registry({ registryUrl: registry.url });

        const stale = await getTrustedPublishingToken(client, 'replaced', 'ext', options);
        await refreshTrustedPublishingToken(client, 'replaced', 'ext', options, stale);
        const again = await refreshTrustedPublishingToken(client, 'replaced', 'ext', options, stale);

        expect(again).toBe('second-token');
        expect(registry.exchanges).toBe(2);
    });
});
