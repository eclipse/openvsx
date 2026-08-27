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
import * as os from 'os';
import * as path from 'path';
import * as yazl from 'yazl';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { verifySignature } from '../../src/verify-signature';

function buildZip(entries: Record<string, Buffer>): Promise<Buffer> {
    return new Promise((resolve, reject) => {
        const zipFile = new yazl.ZipFile();
        for (const [name, content] of Object.entries(entries)) {
            zipFile.addBuffer(content, name);
        }
        const chunks: Buffer[] = [];
        zipFile.outputStream.on('data', chunk => chunks.push(chunk));
        zipFile.outputStream.on('end', () => resolve(Buffer.concat(chunks)));
        zipFile.outputStream.on('error', reject);
        zipFile.end();
    });
}

function sha256Base64(content: Buffer): string {
    return crypto.createHash('sha256').update(content).digest('base64');
}

describe('verify-signature', () => {

    const tmpFiles: string[] = [];
    let logSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);
    });

    afterEach(() => {
        vi.restoreAllMocks();
        tmpFiles.splice(0).forEach(file => fs.rmSync(file, { force: true }));
    });

    function writeTempFile(content: Buffer | string, extension: string): string {
        const file = path.join(os.tmpdir(), `ovsx-verify-signature-test-${Math.random().toString(36).slice(2)}${extension}`);
        fs.writeFileSync(file, content);
        tmpFiles.push(file);
        return file;
    }

    async function givenSignedPackage(entries: Record<string, Buffer>) {
        const packageBytes = await buildZip(entries);
        const packagePath = writeTempFile(packageBytes, '.vsix');

        const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
        const signature = crypto.sign(null, packageBytes, privateKey);
        const publicKeyPem = publicKey.export({ type: 'spki', format: 'pem' }) as string;

        const manifest = {
            package: { size: packageBytes.length, digests: { sha256: sha256Base64(packageBytes) } },
            entries: Object.fromEntries(
                Object.entries(entries).map(([name, content]) => [
                    Buffer.from(name, 'utf-8').toString('base64'),
                    { size: content.length, digests: { sha256: sha256Base64(content) } }
                ])
            )
        };

        return {
            packagePath,
            manifestPath: writeTempFile(JSON.stringify(manifest), '.manifest'),
            signaturePath: writeTempFile(signature, '.sig'),
            publicKeyPath: writeTempFile(publicKeyPem, '.pem'),
            manifest
        };
    }

    it('accepts a valid signature with a matching manifest', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });

        await verifySignature(fixture);

        expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('Signature is valid.'));
    });

    it('rejects a tampered package', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        fs.appendFileSync(fixture.packagePath, Buffer.from('tampered'));

        await expect(verifySignature(fixture)).rejects.toThrow('Signature verification failed.');
    });

    it('rejects a signature made with a different key', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        const { privateKey } = crypto.generateKeyPairSync('ed25519');
        const packageBytes = fs.readFileSync(fixture.packagePath);
        fs.writeFileSync(fixture.signaturePath, crypto.sign(null, packageBytes, privateKey));

        await expect(verifySignature(fixture)).rejects.toThrow('Signature verification failed.');
    });

    it('rejects an unparsable manifest', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        fs.writeFileSync(fixture.manifestPath, 'not json');

        await expect(verifySignature(fixture)).rejects.toThrow('Failed to parse the manifest file as JSON');
    });

    it('reports a package digest mismatch even though the outer signature is valid', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        manifest.package.digests.sha256 = 'not-the-real-digest';
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow('the package digest does not match the manifest');
    });

    it('distinguishes a missing package digest from a mismatched one', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        delete manifest.package.digests.sha256;
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow('the manifest does not record a package digest');
    });

    it('identifies which entry does not match its digest', async () => {
        const fixture = await givenSignedPackage({
            'extension/package.json': Buffer.from('{"name":"bar"}'),
            'extension/README.md': Buffer.from('hello')
        });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        const key = Buffer.from('extension/README.md', 'utf-8').toString('base64');
        manifest.entries[key].digests.sha256 = 'not-the-real-digest';
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow("'extension/README.md' does not match its digest in the manifest");
    });

    it('distinguishes an entry with no recorded digest from one with a mismatched digest', async () => {
        const fixture = await givenSignedPackage({
            'extension/package.json': Buffer.from('{"name":"bar"}'),
            'extension/README.md': Buffer.from('hello')
        });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        const key = Buffer.from('extension/README.md', 'utf-8').toString('base64');
        delete manifest.entries[key].digests.sha256;
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow("'extension/README.md' has no recorded digest in the manifest");
    });

    it('identifies an entry present in the package but missing from the manifest', async () => {
        const fixture = await givenSignedPackage({
            'extension/package.json': Buffer.from('{"name":"bar"}'),
            'extension/README.md': Buffer.from('hello')
        });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        delete manifest.entries[Buffer.from('extension/README.md', 'utf-8').toString('base64')];
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow("'extension/README.md' is present in the package but missing from the manifest");
    });

    it('identifies an entry listed in the manifest but missing from the package', async () => {
        const fixture = await givenSignedPackage({ 'extension/package.json': Buffer.from('{"name":"bar"}') });
        const manifest = JSON.parse(fs.readFileSync(fixture.manifestPath, 'utf-8'));
        manifest.entries[Buffer.from('extension/ghost.txt', 'utf-8').toString('base64')] = {
            size: 1,
            digests: { sha256: 'irrelevant' }
        };
        fs.writeFileSync(fixture.manifestPath, JSON.stringify(manifest));

        await expect(verifySignature(fixture)).rejects.toThrow("'extension/ghost.txt' is listed in the manifest but missing from the package");
    });
});
