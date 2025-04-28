/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import * as yauzl from 'yauzl-promise';
import { Readable } from 'stream';
import { Manifest } from './util';

async function bufferStream(stream: Readable): Promise<Buffer> {
	return await new Promise((resolve, reject) => {
		const buffers: Buffer[] = [];
		stream.on('data', buffer => buffers.push(buffer));
		stream.once('error', reject);
		stream.once('end', () => resolve(Buffer.concat(buffers)));
	});
}

async function readZip(packagePath: string, filter: (name: string) => boolean): Promise<Map<string, Buffer>> {
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

export async function readVSIXPackage(packagePath: string): Promise<Manifest> {
	const map = await readZip(packagePath, name => /^extension\/package\.json$/i.test(name));
	const rawManifest = map.get('extension/package.json');
	if (!rawManifest) {
		throw new Error('Manifest not found.');
	}

	return JSON.parse(rawManifest.toString('utf8')) as Manifest;
}
