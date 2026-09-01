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
import { getTrustedPublishingToken } from '../../src/trusted-publishing';

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
});
