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
import { createVSIX, CreateVSIXOptions, publishVSIX } from '../../src/api';
import { Logger } from '../../src/logger';

// The credential store is never supposed to be consulted by the API, and touching the keychain of the
// machine running the tests would be a side effect either way.
vi.mock('../../src/store', () => ({
    openDefaultStore: async () => ({
        get: async () => undefined,
        add: async () => { },
        delete: async () => { }
    })
}));

interface PublishRequest {
    query: URLSearchParams;
    body: Buffer;
}

interface RegistryService {
    url: string;
    requests: PublishRequest[];
    close: () => Promise<void>;
}

/**
 * Stands in for the registry API endpoint that accepts a packaged extension.
 */
async function startRegistry(responses: { status: number, body: unknown }[]): Promise<RegistryService> {
    const requests: PublishRequest[] = [];
    const server = http.createServer((req, res) => {
        const chunks: Buffer[] = [];
        req.on('data', chunk => chunks.push(chunk));
        req.on('end', () => {
            const url = new URL(req.url ?? '/', 'http://127.0.0.1');
            requests.push({ query: url.searchParams, body: Buffer.concat(chunks) });
            const response = responses[Math.min(requests.length - 1, responses.length - 1)];
            res.writeHead(response.status, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(response.body));
        });
    });
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    const port = (server.address() as AddressInfo).port;
    return {
        url: `http://127.0.0.1:${port}`,
        requests,
        close: () => new Promise<void>(resolve => server.close(() => resolve()))
    };
}

function extensionJson(version: string = '1.0.0', overrides: Record<string, unknown> = {}) {
    return {
        namespace: 'testpub',
        name: 'test-extension',
        version,
        targetPlatform: 'universal',
        ...overrides
    };
}

/**
 * Writes an extension that `vsce` can package without emitting warnings: it declares a repository, so
 * it is not asked for, and ships the LICENSE its manifest promises as well as a `.vscodeignore`.
 */
function writeExtension(directory: string, manifest: Record<string, unknown> = {}): void {
    fs.writeFileSync(path.join(directory, 'package.json'), JSON.stringify({
        name: 'test-extension',
        publisher: 'testpub',
        version: '1.0.0',
        license: 'MIT',
        repository: 'https://github.com/testpub/test-extension',
        engines: { vscode: '^1.57.0' },
        ...manifest
    }));
    fs.writeFileSync(path.join(directory, 'README.md'), '# Test Extension');
    fs.writeFileSync(path.join(directory, 'LICENSE.txt'), 'MIT');
    fs.writeFileSync(path.join(directory, '.vscodeignore'), '.vscodeignore\n');
}

function recordingLogger(): Logger & { messages: string[] } {
    const messages: string[] = [];
    return {
        messages,
        log: (message = '') => messages.push(message),
        warn: message => messages.push(message)
    };
}

describe('publishVSIX', () => {

    const services: RegistryService[] = [];
    const temporaryFiles: string[] = [];
    const temporaryDirectories: string[] = [];
    const environment = { ...process.env };

    beforeEach(() => {
        delete process.env['OVSX_PAT'];
        delete process.env['OVSX_REGISTRY_URL'];
    });

    afterEach(async () => {
        process.env = { ...environment };
        temporaryFiles.splice(0).forEach(file => fs.rmSync(file, { force: true }));
        temporaryDirectories.splice(0).forEach(dir => fs.rmSync(dir, { recursive: true, force: true }));
        await Promise.all(services.splice(0).map(service => service.close()));
    });

    async function givenRegistry(...responses: { status: number, body: unknown }[]): Promise<RegistryService> {
        const service = await startRegistry(responses.length > 0 ? responses : [{ status: 201, body: extensionJson() }]);
        services.push(service);
        return service;
    }

    /**
     * A package that only has to exist, for the cases where the token does not have to be looked up.
     */
    function givenPackage(content: string = 'not really a zip'): string {
        const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'ovsx-test-')), 'test.vsix');
        fs.writeFileSync(file, content);
        temporaryFiles.push(file);
        return file;
    }

    async function givenRealPackage(): Promise<string> {
        const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'ovsx-test-'));
        temporaryDirectories.push(directory);
        writeExtension(directory);
        return createVSIX({ packagePath: directory, dependencies: false });
    }

    it('returns the published extension', async () => {
        const registry = await givenRegistry();
        const log = recordingLogger();

        const published = await publishVSIX(givenPackage(), { registryUrl: registry.url, pat: 'my-pat', log });

        expect(published).toHaveLength(1);
        expect(published[0].namespace).toBe('testpub');
        expect(published[0].name).toBe('test-extension');
        expect(published[0].version).toBe('1.0.0');
        expect(registry.requests).toHaveLength(1);
        expect(registry.requests[0].query.get('token')).toBe('my-pat');
    });

    it('publishes every given package', async () => {
        const registry = await givenRegistry(
            { status: 201, body: extensionJson('1.0.0') },
            { status: 201, body: extensionJson('2.0.0') });

        const published = await publishVSIX(
            [givenPackage(), givenPackage()],
            { registryUrl: registry.url, pat: 'my-pat', log: recordingLogger() });

        expect(published.map(extension => extension.version)).toEqual(['1.0.0', '2.0.0']);
    });

    it('rejects on the first failure instead of settling', async () => {
        const registry = await givenRegistry({ status: 400, body: { error: 'Something went wrong' } });

        await expect(publishVSIX(
            [givenPackage(), givenPackage()],
            { registryUrl: registry.url, pat: 'my-pat', log: recordingLogger() }))
            .rejects.toThrow('Something went wrong');

        expect(registry.requests).toHaveLength(1);
    });

    it('reports an error response as a rejection', async () => {
        const registry = await givenRegistry({ status: 201, body: { error: 'Unknown publisher' } });

        await expect(publishVSIX(givenPackage(), { registryUrl: registry.url, pat: 'my-pat', log: recordingLogger() }))
            .rejects.toThrow('Unknown publisher');
    });

    it('skips an already published version when asked to', async () => {
        const registry = await givenRegistry(
            { status: 400, body: { error: 'Extension testpub.test-extension 1.0.0 is already published.' } });
        const log = recordingLogger();

        const published = await publishVSIX(
            givenPackage(),
            { registryUrl: registry.url, pat: 'my-pat', skipDuplicate: true, log });

        expect(published).toEqual([]);
        expect(log.messages.join('\n')).toContain('Skipping publish');
    });

    it('does not ask for a token but fails with a hint', async () => {
        const registry = await givenRegistry();
        // A real package: without a token the namespace is read from it to look one up.
        const vsix = await givenRealPackage();

        await expect(publishVSIX(vsix, { registryUrl: registry.url, log: recordingLogger() }))
            .rejects.toThrow(/No personal access token found for namespace 'testpub'/);

        expect(registry.requests).toEqual([]);
    });

    it('picks the token up from the environment', async () => {
        const registry = await givenRegistry();
        process.env['OVSX_PAT'] = 'pat-from-env';

        await publishVSIX(givenPackage(), { registryUrl: registry.url, log: recordingLogger() });

        expect(registry.requests[0].query.get('token')).toBe('pat-from-env');
    });

    it('leaves the options of the caller untouched', async () => {
        const registry = await givenRegistry();
        process.env['OVSX_PAT'] = 'pat-from-env';
        const options = { registryUrl: registry.url, log: recordingLogger() };

        await publishVSIX(givenPackage(), options);

        expect(options).not.toHaveProperty('pat');
        expect(options).not.toHaveProperty('extensionFile');
    });

    it('writes progress to the given logger and not to the console', async () => {
        const registry = await givenRegistry();
        const log = recordingLogger();
        const consoleLog = vi.spyOn(console, 'log').mockImplementation(() => { });
        try {
            await publishVSIX(givenPackage(), { registryUrl: registry.url, pat: 'my-pat', log });
        } finally {
            consoleLog.mockRestore();
        }

        expect(log.messages.join('\n')).toContain('Published testpub.test-extension v1.0.0');
        expect(consoleLog).not.toHaveBeenCalled();
    });
});

describe('createVSIX', () => {

    const directories: string[] = [];
    const environment = { ...process.env };

    beforeEach(() => {
        // Keeps vsce from blocking on its confirmation prompt when it has something to complain about,
        // and from reporting warnings as workflow commands instead of to the console.
        process.env['VSCE_TESTS'] = '1';
        delete process.env['GITHUB_ACTIONS'];
    });

    afterEach(() => {
        process.env = { ...environment };
        directories.splice(0).forEach(directory => fs.rmSync(directory, { recursive: true, force: true }));
    });

    /**
     * Collects what vsce reported while packaging.
     */
    async function warningsWhilePackaging(options: CreateVSIXOptions): Promise<string> {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => { });
        try {
            await createVSIX(options);
            return warn.mock.calls.map(call => call.join(' ')).join('\n');
        } finally {
            warn.mockRestore();
        }
    }

    function givenExtension(manifest: Record<string, unknown> = {}): string {
        const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'ovsx-test-'));
        directories.push(directory);
        writeExtension(directory, manifest);
        return directory;
    }

    it('packages the extension and returns its path', async () => {
        const packagePath = givenExtension();

        const vsix = await createVSIX({ packagePath, dependencies: false });

        expect(vsix).toBe(path.join(packagePath, 'test-extension-1.0.0.vsix'));
        expect(fs.existsSync(vsix)).toBe(true);
    });

    it('honours an explicit output path and package version', async () => {
        const packagePath = givenExtension();
        const outputPath = path.join(packagePath, 'out.vsix');

        const vsix = await createVSIX({ packagePath, outputPath, packageVersion: '2.3.4', dependencies: false });

        expect(vsix).toBe(outputPath);
        expect(fs.existsSync(outputPath)).toBe(true);
    });

    it('lets vsce accept a missing repository when asked to', async () => {
        const packagePath = givenExtension({ repository: undefined });

        const warnings = await warningsWhilePackaging(
            { packagePath, dependencies: false, allowMissingRepository: true });

        expect(warnings).not.toContain("'repository' field is missing");
    });

    it('has vsce complain about a missing repository otherwise', async () => {
        const packagePath = givenExtension({ repository: undefined });

        const warnings = await warningsWhilePackaging({ packagePath, dependencies: false });

        expect(warnings).toContain("'repository' field is missing");
    });

    it('rejects an incomplete manifest', async () => {
        const packagePath = givenExtension({ publisher: undefined });

        await expect(createVSIX({ packagePath, dependencies: false })).rejects.toThrow("Missing required field 'publisher'");
    });
});
