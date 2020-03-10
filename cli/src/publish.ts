/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createVSIX } from 'vsce';
import { createTempFile } from './util';
import { Registry } from './registry';

/**
 * Publishes an extension.
 */
export async function publish(options: PublishOptions = {}): Promise<void> {
    if (!options.registryUrl) {
        options.registryUrl = process.env.OVSX_REGISTRY_URL;
    }
    if (!options.pat) {
        options.pat = process.env.OVSX_PAT;
        if (!options.pat) {
            throw new Error("A personal access token must be given with the option '--pat'.");
        }
    }
    if (!options.extensionFile) {
        options.extensionFile = await createTempFile({ postfix: '.vsix' });
        await createVSIX({
            cwd: options.packagePath,
            packagePath: options.extensionFile,
            baseContentUrl: options.baseContentUrl,
            baseImagesUrl: options.baseImagesUrl,
            useYarn: options.yarn
        });
        console.log(); // new line
    }
    const registry = new Registry({ url: options.registryUrl });
    const extension = await registry.publish(options.extensionFile, options.pat);
    if (extension.error) {
        throw new Error(extension.error);
    }
    console.log(`\ud83d\ude80  Published ${extension.namespace}.${extension.name} v${extension.version}`);
}

export interface PublishOptions {
    /**
     * The base URL of the registry API.
     */
    registryUrl?: string;
    /**
     * Personal access token.
     */
    pat?: string;
    /**
     * Path to the vsix file to be published. Cannot be used together with `packagePath`.
     */
    extensionFile?: string;
    /**
     * Path to the extension to be packaged and published. Cannot be used together
     * with `extensionFile`.
     */
    packagePath?: string;
    /**
	 * The base URL for links detected in Markdown files. Only valid with `packagePath`.
	 */
    baseContentUrl?: string;
    /**
	 * The base URL for images detected in Markdown files. Only valid with `packagePath`.
	 */
    baseImagesUrl?: string;
    /**
	 * Should use `yarn` instead of `npm`. Only valid with `packagePath`.
	 */
    yarn?: boolean;
}
