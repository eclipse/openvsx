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

export interface Store {
	get(name: string): Promise<string | undefined>;
	add(name: string, value: string): Promise<void>;
	delete(name: string): Promise<void>;
}

export class FileStore implements Store {
	static readonly DefaultPath = path.join(homedir(), '.ovsx');

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

	async get(name: string): Promise<string | undefined> {
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

export class KeychainStore implements Store {
	static async open(serviceName = 'ovsx'): Promise<KeychainStore> {
		const keychain = await import('cross-keychain');
		// probe the credential store so we can fall back to the file store when it's unusable
		await keychain.getPassword(serviceName, serviceName);

		return new KeychainStore(keychain, serviceName);
	}

	private constructor(
		private readonly keychain: typeof import('cross-keychain'),
		private readonly serviceName: string
	) { }

	async get(name: string): Promise<string | undefined> {
		return await this.keychain.getPassword(this.serviceName, name) ?? undefined;
	}

	async add(name: string, value: string): Promise<void> {
		await this.keychain.setPassword(this.serviceName, name, value);
	}

	async delete(name: string): Promise<void> {
		await this.keychain.deletePassword(this.serviceName, name);
	}
}

export async function openDefaultStore(): Promise<Store> {
	if (/^file$/i.test(process.env['OVSX_STORE'] ?? '')) {
		console.warn(`!!  Storing secrets clear-text in '${FileStore.DefaultPath}' (not recommended). Unset OVSX_STORE to use the system credential store instead.`);
		return await FileStore.open();
	}

	let keychainStore: Store;
	try {
		keychainStore = await KeychainStore.open();
	} catch (err) {
		console.warn(`Failed to open the system credential store: ${err.message}`);
		console.warn(`!!  Falling back to storing secrets clear-text in '${FileStore.DefaultPath}' (not recommended).`);
		return await FileStore.open();
	}

	const fileStore = await FileStore.open();

	// migrate from file store
	if (fileStore.size) {
		for (const { name, value } of fileStore) {
			await keychainStore.add(name, value);
		}

		await fileStore.deleteStore();
		console.info(`Migrated ${fileStore.size} publishers to system credential manager. Deleted local store '${fileStore.path}'.`);
	}

	return keychainStore;
}
