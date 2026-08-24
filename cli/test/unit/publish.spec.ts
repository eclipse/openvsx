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

import * as fs from 'fs';
import * as http from 'http';
import * as os from 'os';
import * as path from 'path';
import { AddressInfo } from 'net';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { publish } from '../../src/publish';

interface RecordedRequest {
    pathname: string;
    query: URLSearchParams;
}

interface RegistryStub {
    url: string;
    publishRequests: RecordedRequest[];
    close: () => Promise<void>;
}

/**
 * Stands in for the registry's `/api/version` and `/api/-/publish` endpoints.
 */
async function startRegistryStub(
    version: { status?: number; body?: unknown } = {},
    publishResponse: { status?: number; body?: unknown } = {}
): Promise<RegistryStub> {
    const publishRequests: RecordedRequest[] = [];
    const versionStatus = version.status ?? 200;
    const versionBody = version.body ?? { version: '1.2.0' };
    const publishStatus = publishResponse.status ?? 200;
    const publishBody = publishResponse.body ?? {
        namespace: 'foo',
        name: 'bar',
        version: '1.0.0',
        targetPlatform: 'universal'
    };
    const server = http.createServer((req, res) => {
        const url = new URL(req.url ?? '/', 'http://127.0.0.1');
        req.on('data', () => undefined);
        req.on('end', () => {
            if (url.pathname === '/api/version') {
                res.writeHead(versionStatus, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(versionBody));
            } else {
                publishRequests.push({ pathname: url.pathname, query: url.searchParams });
                res.writeHead(publishStatus, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(publishBody));
            }
        });
    });
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    const port = (server.address() as AddressInfo).port;
    return {
        url: `http://127.0.0.1:${port}`,
        publishRequests,
        close: () => new Promise<void>(resolve => server.close(() => resolve()))
    };
}

describe('publish', () => {

    const stubs: RegistryStub[] = [];
    const tmpFiles: string[] = [];

    beforeEach(() => {
        vi.spyOn(console, 'log').mockImplementation(() => undefined);
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    });

    afterEach(async () => {
        vi.restoreAllMocks();
        await Promise.all(stubs.splice(0).map(stub => stub.close()));
        tmpFiles.splice(0).forEach(file => fs.rmSync(file, { force: true }));
    });

    async function givenRegistry(
        version?: { status?: number; body?: unknown },
        publishResponse?: { status?: number; body?: unknown }
    ): Promise<RegistryStub> {
        const stub = await startRegistryStub(version, publishResponse);
        stubs.push(stub);
        return stub;
    }

    function givenExtensionFile(sizeInBytes: number): string {
        const file = path.join(os.tmpdir(), `ovsx-publish-test-${Math.random().toString(36).slice(2)}.vsix`);
        fs.writeFileSync(file, Buffer.alloc(sizeInBytes));
        tmpFiles.push(file);
        return file;
    }

    it('publishes a package that is within the registry size limit', async () => {
        const registry = await givenRegistry({ body: { version: '1.2.0', maxExtensionSize: 1024 } });
        const extensionFile = givenExtensionFile(100);

        const [result] = await publish({ extensionFile, pat: 'the.pat', registryUrl: registry.url });

        expect(result.status).toBe('fulfilled');
        expect(registry.publishRequests).toHaveLength(1);
    });

    it('rejects locally, without uploading, when the package exceeds the registry size limit', async () => {
        const registry = await givenRegistry({ body: { version: '1.2.0', maxExtensionSize: 100 } });
        const extensionFile = givenExtensionFile(200);

        const [result] = await publish({ extensionFile, pat: 'the.pat', registryUrl: registry.url });

        expect(result.status).toBe('rejected');
        expect((result as PromiseRejectedResult).reason.message).toBe(
            `The extension package (200 bytes) exceeds the size limit of 100 bytes accepted by the registry at ${registry.url}.`
        );
        expect(registry.publishRequests).toHaveLength(0);
    });

    it('proceeds when the registry does not report a size limit', async () => {
        const registry = await givenRegistry({ body: { version: '1.2.0' } });
        const extensionFile = givenExtensionFile(200);

        const [result] = await publish({ extensionFile, pat: 'the.pat', registryUrl: registry.url });

        expect(result.status).toBe('fulfilled');
        expect(registry.publishRequests).toHaveLength(1);
    });

    it('proceeds when the registry does not expose `/api/version`', async () => {
        const registry = await givenRegistry({ status: 404, body: {} });
        const extensionFile = givenExtensionFile(200);

        const [result] = await publish({ extensionFile, pat: 'the.pat', registryUrl: registry.url });

        expect(result.status).toBe('fulfilled');
        expect(registry.publishRequests).toHaveLength(1);
    });
});
