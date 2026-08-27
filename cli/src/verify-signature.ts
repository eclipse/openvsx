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
import { readAllZipEntries } from './zip';
import { VerifySignatureOptions } from './verify-signature-options';

interface DigestEntry {
    size?: number;
    digests?: { sha256?: string };
}

interface SignatureManifest {
    package?: DigestEntry;
    entries?: { [base64Name: string]: DigestEntry };
}

/**
 * Verifies a package/manifest/signature file trio entirely offline, the same way `vsce
 * verify-signature` does - no registry involved, every input is a local file. Unlike `verify`,
 * which fetches everything it needs from a registry for a package identified by name, this is for
 * a signature archive you already extracted yourself (e.g. the `.sigzip` a registry serves has a
 * `.signature.sig` and a `.signature.manifest` entry, matching `--signaturePath` and
 * `--manifestPath` here).
 *
 * `vsce verify-signature` validates against Microsoft's own (closed-source) signing scheme and
 * has no need for a public key argument - VS Code's Marketplace keys are baked into its verifier.
 * Open VSX's registries each hold their own Ed25519 key pair instead, so `--publicKeyPath` is
 * required here to say which registry's signature this is supposed to be.
 */
export async function verifySignature(options: VerifySignatureOptions): Promise<void> {
    const [packageBytes, manifestJson, signature, publicKeyPem] = await Promise.all([
        fs.promises.readFile(options.packagePath),
        fs.promises.readFile(options.manifestPath, 'utf-8'),
        fs.promises.readFile(options.signaturePath),
        fs.promises.readFile(options.publicKeyPath, 'utf-8')
    ]);

    let manifest: SignatureManifest;
    try {
        manifest = JSON.parse(manifestJson);
    } catch (err) {
        throw new Error(`Failed to parse the manifest file as JSON: ${err instanceof Error ? err.message : err}`);
    }

    const publicKey = crypto.createPublicKey({ key: publicKeyPem, format: 'pem' });
    const signatureValid = crypto.verify(null, packageBytes, publicKey, signature);
    if (!signatureValid) {
        throw new Error('Signature verification failed. The package may have been tampered with.');
    }

    const manifestIssues = await checkManifest(options.packagePath, packageBytes, manifest);
    if (manifestIssues.length > 0) {
        throw new Error(
            'The signature is valid, but the package does not match the manifest:\n'
            + manifestIssues.map(issue => `  - ${issue}`).join('\n')
        );
    }

    console.log('✅  Signature is valid.');
}

function sha256Base64(content: Buffer): string {
    return crypto.createHash('sha256').update(content).digest('base64');
}

/**
 * The manifest itself isn't part of what the signature covers - only the raw package bytes are
 * signed - so this can't add cryptographic trust on its own. It does confirm the manifest wasn't
 * corrupted or is stale relative to the actual package, and unlike the signature check alone, can
 * point at exactly which entry doesn't match.
 */
async function checkManifest(packagePath: string, packageBytes: Buffer, manifest: SignatureManifest): Promise<string[]> {
    const issues: string[] = [];

    const actualPackageDigest = sha256Base64(packageBytes);
    if (manifest.package?.digests?.sha256 !== actualPackageDigest) {
        issues.push('the package digest does not match the manifest');
    }

    const manifestEntries = manifest.entries ?? {};
    const seenKeys = new Set<string>();

    const packageEntries = await readAllZipEntries(packagePath);
    for (const [name, content] of packageEntries) {
        const key = Buffer.from(name, 'utf-8').toString('base64');
        seenKeys.add(key);
        const entry = manifestEntries[key];
        if (!entry) {
            issues.push(`'${name}' is present in the package but missing from the manifest`);
            continue;
        }
        if (entry.digests?.sha256 !== sha256Base64(content)) {
            issues.push(`'${name}' does not match its digest in the manifest`);
        }
    }

    for (const key of Object.keys(manifestEntries)) {
        if (!seenKeys.has(key)) {
            const name = Buffer.from(key, 'base64').toString('utf-8');
            issues.push(`'${name}' is listed in the manifest but missing from the package`);
        }
    }

    return issues;
}
