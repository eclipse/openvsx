/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import { Entry, open, ZipFile } from 'yauzl';
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

async function openZip(packagePath: string) {
	return new Promise<ZipFile>((resolve, reject) =>
		open(packagePath, { lazyEntries: true }, (err: Error | null, zipfile: ZipFile) => (err ? reject(err) : resolve(zipfile)))
	);
}

async function readZip(packagePath: string, filter: (name: string) => boolean): Promise<Map<string, Buffer>> {
	const zipfile = await openZip(packagePath);

	return await new Promise((resolve, reject) => {
		const result = new Map<string, Buffer>();
		zipfile.once('close', () => resolve(result));

		zipfile.readEntry();
		zipfile.on('streamEntry', (entry: Entry) => {
			zipfile.openReadStream(entry, (err: Error | null, stream: Readable) => {
				if (err) {
					zipfile.close();
					return reject(err);
				}

				bufferStream(stream).then(buffer => {
					const name = entry.fileName.toLowerCase();
					result.set(name, buffer);
					zipfile.readEntry();
				});
			});
		});
		zipfile.on('entry', (entry: Entry) => {
			const name = entry.fileName.toLowerCase();
			if (filter(name)) {
				zipfile.emit('streamEntry', entry);
			} else {
				zipfile.readEntry();
			}
		});
	});
}

export async function readVSIXPackage(packagePath: string): Promise<Manifest> {
	const map = await readZip(packagePath, name => /^extension\/package\.json$/i.test(name));
	const rawManifest = map.get('extension/package.json');
	if (!rawManifest) {
		throw new Error('Manifest not found.');
	}

    return JSON.parse(rawManifest.toString('utf8')) as Manifest;
}
