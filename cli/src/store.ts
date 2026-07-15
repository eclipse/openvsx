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

async function verifyBackend(keychain: typeof import('cross-keychain'), serviceName: string, description: string): Promise<void> {
	// Probe the active backend so we fail fast when it's present but unusable (e.g.
	// `security`/`secret-tool` exists but the keychain is locked or the daemon is down).
	try {
		await keychain.getPassword(serviceName, serviceName);
	} catch (err) {
		throw new Error(`${description} is unavailable: ${err.message}`);
	}
}

/** Shared behavior for stores backed by the cross-keychain library. */
abstract class CrossKeychainStore implements Store {
	protected constructor(
		protected readonly keychain: typeof import('cross-keychain'),
		protected readonly serviceName: string
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

export class KeychainStore extends CrossKeychainStore {
	static async open(serviceName = 'ovsx'): Promise<KeychainStore> {
		const keychain = await import('cross-keychain');

		// cross-keychain auto-detects the highest-priority backend for this platform.
		const { id, name } = await keychain.diagnose();
		if (id === 'file' || id === 'null') {
			throw new Error('No OS credential store detected on this platform.');
		}

		await verifyBackend(keychain, serviceName, `System credential store '${name}'`);
		return new KeychainStore(keychain, serviceName);
	}
}

export class EncryptedFileStore extends CrossKeychainStore {
    static async open(serviceName = 'ovsx'): Promise<EncryptedFileStore> {
		const keychain = await import('cross-keychain');

		await keychain.useBackend('file');
		await verifyBackend(keychain, serviceName, 'Encrypted file store');
		return new EncryptedFileStore(keychain, serviceName);
	}
}

export async function openDefaultStore(): Promise<Store> {
	// OVSX_STORE=file forces the encrypted file store and skips the OS credential store.
	const forceFile = /^file$/i.test(process.env['OVSX_STORE'] ?? '');

	let store: KeychainStore | EncryptedFileStore | undefined;

	// Prefer the operating system's credential store.
	if (!forceFile) {
		try {
			store = await KeychainStore.open();
		} catch (err) {
			console.warn(`WARN: ${err.message}`);
		}
	}

	// Otherwise fall back to cross-keychain's encrypted file store.
	if (!store) {
		try {
			store = await EncryptedFileStore.open();
			if (!forceFile) {
				console.warn(`WARN: Storing secrets in cross-keychain's encrypted file store as system keychain is not available..`);
			}
		} catch (err) {
            console.warn(`WARN: ${err.message}`);
		}
	}

	// Last resort: if no cross-keychain store works, keep publishing usable by storing secrets clear-text.
	if (!store) {
		console.warn(`WARN: Falling back to storing secrets clear-text at '${FileStore.DefaultPath}' (not recommended).`);
		return await FileStore.open();
	}

    try {
        await migrateLegacyStore(store);
    } catch (err) {
        console.warn(`WARN: Failed to read legacy file store at '${FileStore.DefaultPath}': ${err.message}`);
        console.warn(`WARN: Skipping migration of Legacy file store.`);
    }

	return store;
}

// Migrate secrets from the legacy clear-text file store into the given store, then delete it.
async function migrateLegacyStore(target: Store): Promise<void> {
	const fileStore = await FileStore.open();
	if (!fileStore.size) {
		return;
	}

	const migrated = fileStore.size;
	for (const { name, value } of fileStore) {
		await target.add(name, value);
	}

	await fileStore.deleteStore();
	console.info(`INFO: Migrated ${migrated} publishers to the credential store. Deleted local store '${fileStore.path}'.`);
}
