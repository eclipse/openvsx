/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import * as crypto from 'crypto';
import * as yauzl from 'yauzl-promise';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';
import { Manifest } from './util';

async function bufferStream(stream: Readable): Promise<Buffer> {
	return await new Promise((resolve, reject) => {
		const buffers: Buffer[] = [];
		stream.on('data', buffer => buffers.push(buffer));
		stream.once('error', reject);
		stream.once('end', () => resolve(Buffer.concat(buffers)));
	});
}

export async function readZip(packagePath: string, filter: (name: string) => boolean): Promise<Map<string, Buffer>> {
	const result = new Map<string, Buffer>();
	const zipfile = await yauzl.open(packagePath);
	try {
		for await (const entry of zipfile) {
			const name = entry.filename.toLowerCase();
			if (filter(name)) {
				const stream = await zipfile.openReadStream(entry);
				const buffer = await bufferStream(stream);
				result.set(name, buffer);
			}
		}
	} finally {
		await zipfile.close();
	}

	return result;
}

/**
 * Computes the SHA256 digest of every non-directory entry of a zip file, keyed by its exact
 * (case-preserving) name - unlike {@link readZip}, which lowercases names for its
 * case-insensitive-filter callers. Used where entry names and digests are compared against another
 * source of truth, such as a signature manifest that records the original names, without also
 * having to hold every entry's full content in memory at once alongside it - each entry's content
 * is streamed straight into a hash and discarded, one entry at a time.
 */
export async function hashAllZipEntries(packagePath: string): Promise<Map<string, string>> {
	const result = new Map<string, string>();
	const zipfile = await yauzl.open(packagePath);
	try {
		for await (const entry of zipfile) {
			if (entry.filename.endsWith('/')) {
				continue;
			}
			const stream = await zipfile.openReadStream(entry);
			const hash = crypto.createHash('sha256');
			await pipeline(stream, hash);
			result.set(entry.filename, hash.digest('base64'));
		}
	} finally {
		await zipfile.close();
	}

	return result;
}

export async function readVSIXPackage(packagePath: string): Promise<Manifest> {
	const map = await readZip(packagePath, name => /^extension\/package\.json$/i.test(name));
	const rawManifest = map.get('extension/package.json');
	if (!rawManifest) {
		throw new Error('Manifest not found.');
	}

	return JSON.parse(rawManifest.toString('utf8')) as Manifest;
}
