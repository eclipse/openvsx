/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import * as crypto from 'crypto';
import * as fs from 'fs';
import * as http from 'http';
import * as os from 'os';
import * as path from 'path';
import { AddressInfo } from 'net';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { verify } from '../../src/verify';
import { buildZip } from './support/zip';

interface RegistryStub {
    url: string;
    close: () => Promise<void>;
}

/**
 * Builds a zip archive in memory from a set of entry name -> content pairs.
 */

/**
 * Stands in for the registry's extension metadata endpoint plus the signature/public-key file
 * downloads it points to.
 */
async function startRegistryStub(routes: Record<string, { body: Buffer | string; contentType?: string }>): Promise<RegistryStub> {
    const server = http.createServer((req, res) => {
        const url = new URL(req.url ?? '/', 'http://127.0.0.1');
        const route = routes[url.pathname];
        if (!route) {
            res.writeHead(404);
            res.end();
            return;
        }
        res.writeHead(200, { 'Content-Type': route.contentType ?? 'application/json' });
        res.end(route.body);
    });
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    const port = (server.address() as AddressInfo).port;
    return {
        url: `http://127.0.0.1:${port}`,
        close: () => new Promise<void>(resolve => server.close(() => resolve()))
    };
}

describe('verify', () => {

    const stubs: RegistryStub[] = [];
    const tmpFiles: string[] = [];
    let logSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);
    });

    afterEach(async () => {
        vi.restoreAllMocks();
        await Promise.all(stubs.splice(0).map(stub => stub.close()));
        tmpFiles.splice(0).forEach(file => fs.rmSync(file, { force: true }));
    });

    function writeTempFile(content: Buffer, extension: string): string {
        const file = path.join(os.tmpdir(), `ovsx-verify-test-${Math.random().toString(36).slice(2)}${extension}`);
        fs.writeFileSync(file, content);
        tmpFiles.push(file);
        return file;
    }

    async function givenVsixPackage(manifest: { publisher: string; name: string; version: string }): Promise<{ path: string; bytes: Buffer }> {
        const zip = await buildZip({ 'extension/package.json': Buffer.from(JSON.stringify(manifest)) });
        return { path: writeTempFile(zip, '.vsix'), bytes: zip };
    }

    function signPackage(packageBytes: Buffer) {
        const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
        const signature = crypto.sign(null, packageBytes, privateKey);
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' }) as string;
        return { signature, publicKeyPem };
    }

    async function givenSignedRegistry(
        manifest: { publisher: string; name: string; version: string },
        packageBytes: Buffer,
        options: { tamperSignature?: boolean; wrongKey?: boolean; unsigned?: boolean } = {}
    ): Promise<RegistryStub> {
        const metadataPath = `/api/${manifest.publisher}/${manifest.name}`;
        // The stub reads from this object at request time, so its routes can still be filled in
        // with URLs pointing back at the stub itself after the server has actually started.
        const routes: Record<string, { body: Buffer | string; contentType?: string }> = {};
        const stub = await startRegistryStub(routes);
        stubs.push(stub);

        if (options.unsigned) {
            routes[metadataPath] = {
                body: JSON.stringify({
                    namespace: manifest.publisher,
                    name: manifest.name,
                    version: manifest.version,
                    files: {},
                    allVersions: { [manifest.version]: `${stub.url}${metadataPath}` }
                })
            };
        } else {
            const { signature, publicKeyPem } = signPackage(packageBytes);
            const actualSignature = options.tamperSignature ? Buffer.from(signature).reverse() : signature;
            const actualPublicKeyPem = options.wrongKey ? signPackage(Buffer.from('other')).publicKeyPem : publicKeyPem;

            routes['/sigzip'] = { body: await buildZip({ '.signature.sig': actualSignature }), contentType: 'application/octet-stream' };
            routes['/publickey'] = { body: actualPublicKeyPem, contentType: 'text/plain' };
            routes[metadataPath] = {
                body: JSON.stringify({
                    namespace: manifest.publisher,
                    name: manifest.name,
                    version: manifest.version,
                    files: { signature: `${stub.url}/sigzip`, publicKey: `${stub.url}/publickey` },
                    allVersions: { [manifest.version]: `${stub.url}${metadataPath}` }
                })
            };
        }

        return stub;
    }

    it('reports a valid signature as verified', async () => {
        const manifest = { publisher: 'foo', name: 'bar', version: '1.0.0' };
        const pkg = await givenVsixPackage(manifest);
        const registry = await givenSignedRegistry(manifest, pkg.bytes);

        await verify({ packagePath: pkg.path, registryUrl: registry.url });

        expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('This package is identical to foo.bar v1.0.0'));
    });

    it('rejects a tampered signature', async () => {
        const manifest = { publisher: 'foo', name: 'bar', version: '1.0.0' };
        const pkg = await givenVsixPackage(manifest);
        const registry = await givenSignedRegistry(manifest, pkg.bytes, { tamperSignature: true });

        await expect(verify({ packagePath: pkg.path, registryUrl: registry.url }))
            .rejects.toThrow('foo.bar v1.0.0 does not match the version published to');
    });

    it('rejects a signature made with a different key', async () => {
        const manifest = { publisher: 'foo', name: 'bar', version: '1.0.0' };
        const pkg = await givenVsixPackage(manifest);
        const registry = await givenSignedRegistry(manifest, pkg.bytes, { wrongKey: true });

        await expect(verify({ packagePath: pkg.path, registryUrl: registry.url }))
            .rejects.toThrow('foo.bar v1.0.0 does not match the version published to');
    });

    it('resolves the package\'s own version via allVersions when it is not the latest', async () => {
        const manifest = { publisher: 'foo', name: 'bar', version: '1.0.0' };
        const pkg = await givenVsixPackage(manifest);

        const { signature, publicKeyPem } = signPackage(pkg.bytes);
        const sigzip = await buildZip({ '.signature.sig': signature });
        const routes: Record<string, { body: Buffer | string; contentType?: string }> = {
            '/sigzip': { body: sigzip, contentType: 'application/octet-stream' },
            '/publickey': { body: publicKeyPem, contentType: 'text/plain' }
        };
        const stub = await startRegistryStub(routes);
        stubs.push(stub);

        routes['/api/foo/bar'] = {
            body: JSON.stringify({
                namespace: 'foo',
                name: 'bar',
                version: '2.0.0', // the "latest" version - not the one we're verifying
                files: {},
                allVersions: { '1.0.0': `${stub.url}/api/foo/bar/1.0.0`, '2.0.0': `${stub.url}/api/foo/bar` }
            })
        };
        routes['/api/foo/bar/1.0.0'] = {
            body: JSON.stringify({
                namespace: 'foo',
                name: 'bar',
                version: '1.0.0',
                files: { signature: `${stub.url}/sigzip`, publicKey: `${stub.url}/publickey` },
                allVersions: {}
            })
        };

        await verify({ packagePath: pkg.path, registryUrl: stub.url });

        expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('This package is identical to foo.bar v1.0.0'));
    });

    it('fails fast when the version is not signed by the registry', async () => {
        const manifest = { publisher: 'foo', name: 'bar', version: '1.0.0' };
        const pkg = await givenVsixPackage(manifest);
        const registry = await givenSignedRegistry(manifest, pkg.bytes, { unsigned: true });

        await expect(verify({ packagePath: pkg.path, registryUrl: registry.url }))
            .rejects.toThrow('foo.bar v1.0.0 is not signed by the registry - there is nothing to verify.');
    });
});
