/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { describe, it, expect, afterEach, vi } from 'vitest';
import * as http from 'node:http';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import { AddressInfo } from 'node:net';
import { Registry } from '../../src/registry';

// registry.ts imports 'fs', so the mock has to name the same specifier. Everything but
// createWriteStream is the real module; that one is wrapped only to record when the file closes.
const recorder = vi.hoisted(() => ({ events: [] as string[] }));
vi.mock('fs', async importOriginal => {
    const actual = await importOriginal<typeof import('fs')>();
    return {
        ...actual,
        createWriteStream: (...args: Parameters<typeof actual.createWriteStream>) => {
            const stream = actual.createWriteStream(...args);
            stream.on('close', () => recorder.events.push('file closed'));
            return stream;
        }
    };
});

describe('Registry.download', () => {

    const servers: http.Server[] = [];
    const files: string[] = [];

    afterEach(async () => {
        for (const file of files.splice(0)) {
            fs.rmSync(file, { force: true });
        }
        for (const server of servers.splice(0)) {
            await new Promise<void>(resolve => server.close(() => resolve()));
        }
    });

    async function serve(handler: http.RequestListener): Promise<string> {
        const server = http.createServer(handler);
        servers.push(server);
        await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
        return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    }

    function tempPath(suffix: string): string {
        const file = path.join(os.tmpdir(), `ovsx-download-test-${process.pid}-${Math.random().toString(36).slice(2)}${suffix}`);
        files.push(file);
        return file;
    }

    // The promise used to settle when the response ended, which says nothing about the file: a write
    // stream opens and flushes asynchronously, so a caller reading the path straight afterwards could
    // find it empty, or not yet created at all. Small bodies are the worst case, because the response
    // is over before the filesystem has caught up - which is why the tiny public key download was the
    // one that kept failing.
    //
    // Asserted as an ordering rather than by racing it: the file being closed must come first. Timing
    // the race directly makes for a test that only sometimes notices, since it reproduces a few times
    // in a hundred.
    it('resolves only after the file has been closed, not when the response ends', async () => {
        const body = '-----BEGIN PUBLIC KEY-----\nMCowBQYDK2VwAyEA\n-----END PUBLIC KEY-----\n';
        const url = await serve((_, res) => {
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            res.end(body);
        });
        const registry = new Registry({ registryUrl: url });
        const file = tempPath('.pem');

        recorder.events.length = 0;
        await registry.download(file, new URL(`${url}/publickey`));
        recorder.events.push('download resolved');

        expect(recorder.events).toEqual(['file closed', 'download resolved']);
        expect(fs.readFileSync(file, 'utf-8')).toBe(body);
    });

    // An error response used to be piped into the file before the promise rejected, leaving the error
    // page behind at the path the caller had been given.
    it('rejects without writing the error response into the file', async () => {
        const url = await serve((_, res) => {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('no such thing');
        });
        const registry = new Registry({ registryUrl: url });
        const file = tempPath('.pem');

        await expect(registry.download(file, new URL(`${url}/missing`))).rejects.toThrow('status 404');

        const written = fs.existsSync(file) ? fs.readFileSync(file, 'utf-8') : '';
        expect(written).not.toContain('no such thing');
    });
});
