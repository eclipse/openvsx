/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import * as fs from 'fs';
import * as path from 'path';
import { homedir } from 'os';

interface StoreEntry {
    name: string
    value: string
}

export interface Store extends Iterable<StoreEntry> {
	readonly size: number;
	get(name: string): string | undefined;
	add(name: string, value: string): Promise<void>;
	delete(name: string): Promise<void>;
}

export class FileStore implements Store {
	private static readonly DefaultPath = path.join(homedir(), '.ovsx');

	static async open(path: string = FileStore.DefaultPath): Promise<FileStore> {
		try {
			const rawStore = await fs.promises.readFile(path, 'utf8');
			return new FileStore(path, JSON.parse(rawStore).entries);
		} catch (err: any) {
			if (err.code === 'ENOENT') {
				return new FileStore(path, []);
			} else if (/SyntaxError/.test(err)) {
				throw new Error(`Error parsing file store: ${path}.`);
			}

			throw err;
		}
	}

	get size(): number {
		return this.entries.length;
	}

	private constructor(readonly path: string, private entries: StoreEntry[]) { }

	private async save(): Promise<void> {
		await fs.promises.writeFile(this.path, JSON.stringify({ entries: this.entries }), { mode: '0600' });
	}

	async deleteStore(): Promise<void> {
		try {
			await fs.promises.unlink(this.path);
		} catch {
			// noop
		}
	}

	get(name: string): string | undefined {
        return this.entries.find(p => p.name === name)?.value;
	}

	async add(name: string, value: string): Promise<void> {
        const newEntry: StoreEntry = { name, value };
		this.entries = [...this.entries.filter(p => p.name !== name), newEntry];
		await this.save();
	}

	async delete(name: string): Promise<void> {
		this.entries = this.entries.filter(p => p.name !== name);
		await this.save();
	}

	[Symbol.iterator]() {
		return this.entries[Symbol.iterator]();
	}
}

export class KeytarStore implements Store {
	static async open(serviceName = 'ovsx'): Promise<KeytarStore> {
		const keytar = await import('keytar');
		const creds = await keytar.findCredentials(serviceName);

		return new KeytarStore(
			keytar,
			serviceName,
			creds.map(({ account, password }) => ({ name: account, value: password }))
		);
	}

	get size(): number {
		return this.entries.length;
	}

	private constructor(
		private readonly keytar: typeof import('keytar'),
		private readonly serviceName: string,
		private entries: StoreEntry[]
	) { }

	get(name: string): string | undefined {
        return this.entries.find(p => p.name === name)?.value;
	}

	async add(name: string, value: string): Promise<void> {
        const newEntry: StoreEntry = { name, value };
		this.entries = [...this.entries.filter(p => p.name !== name), newEntry];
		await this.keytar.setPassword(this.serviceName, name, value);
	}

	async delete(name: string): Promise<void> {
		this.entries = this.entries.filter(p => p.name !== name);
		await this.keytar.deletePassword(this.serviceName, name);
	}

	[Symbol.iterator](): Iterator<StoreEntry, any, undefined> {
		return this.entries[Symbol.iterator]();
	}
}

export async function openDefaultStore(): Promise<Store> {
	if (/^file$/i.test(process.env['OVSX_STORE'] ?? '')) {
		return await FileStore.open();
	}

	let keytarStore: Store;
	try {
		keytarStore = await KeytarStore.open();
	} catch (err) {
		const store = await FileStore.open();
		console.warn(`Failed to open credential store. Falling back to storing secrets clear-text in: ${store.path}.`);
		return store;
	}

	const fileStore = await FileStore.open();

	// migrate from file store
	if (fileStore.size) {
		for (const { name, value } of fileStore) {
			await keytarStore.add(name, value);
		}

		await fileStore.deleteStore();
		console.info(`Migrated ${fileStore.size} publishers to system credential manager. Deleted local store '${fileStore.path}'.`);
	}

	return keytarStore;
}