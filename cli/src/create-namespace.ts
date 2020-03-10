/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { Registry } from './registry';

/**
 * Creates a namespace (corresponds to `publisher` in package.json).
 */
export async function createNamespace(options: CreateNamespaceOptions = {}): Promise<void> {
    if (!options.registryUrl) {
        options.registryUrl = process.env.OVSX_REGISTRY_URL;
    }
    if (!options.name) {
        throw new Error('The namespace name is mandatory.');
    }
    if (!options.pat) {
        options.pat = process.env.OVSX_PAT;
        if (!options.pat) {
            throw new Error("A personal access token must be given with the option '--pat'.");
        }
    }
    const registry = new Registry({ url: options.registryUrl });
    const result = await registry.createNamespace(options.name, options.pat);
    if (result.error) {
        throw new Error(result.error);
    }
    console.log(`\ud83d\ude80  Created namespace ${options.name}`);
}

export interface CreateNamespaceOptions {
    /**
     * The base URL of the registry API.
     */
    registryUrl?: string;
    /**
     * Personal access token.
     */
    pat?: string;
    /**
     * Name of the new namespace.
     */
    name?: string
}
