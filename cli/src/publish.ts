/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as fs from 'fs';
import * as path from 'path';
import { createVSIX } from 'vsce';
import { createTempFile, readManifest, writeManifest, booleanQuestion } from './util';
import { Registry, DEFAULT_URL } from './registry';

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
    const registry = new Registry({ url: options.registryUrl });
    if (!options.extensionFile) {
        await packageExtension(options, registry);
        console.log(); // new line
    }
    const extension = await registry.publish(options.extensionFile!, options.pat);
    if (extension.error) {
        throw new Error(extension.error);
    }
    console.log(`\ud83d\ude80  Published ${extension.namespace}.${extension.name} v${extension.version}`);
    if (registry.url === DEFAULT_URL) {
        console.log('The open-vsx.org website will be transferred to the Eclipse Foundation on December 9. '
            + 'Please read the blog post referenced below to find out more. '
            + 'Some action will be required so you can continue publishing.\n'
            + 'https://blogs.eclipse.org/post/brian-king/open-vsx-registry-under-new-management');
    }
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

async function packageExtension(options: PublishOptions, registry: Registry): Promise<void> {
    if (registry.url === DEFAULT_URL) {
        // The default registry requires extensions to have a license
        if (!await hasLicenseFile(options.packagePath)) {
            const manifest = await readManifest(options.packagePath);
            if (!manifest.publisher) {
                throw new Error("Missing required field 'publisher'.");
            }
            if (!manifest.name) {
                throw new Error("Missing required field 'name'.");
            }
            if (!manifest.license) {
                const answer = await booleanQuestion(`Extension ${manifest.publisher}.${manifest.name} has no license. Would you like to publish it under the MIT license?`);
                if (answer) {
                    manifest.license = 'MIT';
                    writeManifest(manifest, options.packagePath);
                } else {
                    throw new Error('This extension cannot be accepted because it has no license.');
                }
            }
        }
    }

    options.extensionFile = await createTempFile({ postfix: '.vsix' });
    await createVSIX({
        cwd: options.packagePath,
        packagePath: options.extensionFile,
        baseContentUrl: options.baseContentUrl,
        baseImagesUrl: options.baseImagesUrl,
        useYarn: options.yarn
    });
}

const LICENSE_FILE_NAMES = ['LICENSE.md', 'LICENSE', 'LICENSE.txt'];

async function hasLicenseFile(packagePath?: string): Promise<boolean> {
    for (const fileName of LICENSE_FILE_NAMES) {
        const promise = new Promise(resolve => fs.access(
            path.join(packagePath || '.', fileName),
            err => resolve(!err)
        ));
        if (await promise) {
            return true;
        }
    }
    return false;
}
