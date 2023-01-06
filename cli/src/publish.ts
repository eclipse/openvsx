/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import { createVSIX, IPackageOptions } from '@vscode/vsce';
import { createTempFile, addEnvOptions } from './util';
import { Extension, Registry, RegistryOptions } from './registry';
import { checkLicense } from './check-license';

/**
 * Publishes an extension.
 */
export async function publish(options: PublishOptions = {}): Promise<PromiseSettledResult<void>[]> {
        addEnvOptions(options);
        const internalPublishOptions: InternalPublishOptions[] = [];
        const packagePaths = options.packagePath || [undefined];
        const targets = options.targets || [undefined];
        for (const packagePath of packagePaths) {
            for (const target of targets) {
                internalPublishOptions.push({ ... options, packagePath: packagePath, target: target });
            }
        }

        return Promise.allSettled(internalPublishOptions.map(publishOptions => doPublish(publishOptions)));
}

async function doPublish(options: InternalPublishOptions = {}): Promise<void> {
    if (!options.pat) {
        throw new Error("A personal access token must be given with the option '--pat'.");
    }

    // if the packagePath is a link to a vsix, don't need to package it
    if (options.packagePath && options.packagePath.endsWith('.vsix')) {
        options.extensionFile = options.packagePath;
        delete options.packagePath;
        delete options.target;
    }
    const registry = new Registry(options);
    if (!options.extensionFile) {
        await packageExtension(options, registry);
        console.log(); // new line
    } else if (options.preRelease) {
        console.warn("Ignoring option '--pre-release' for prepackaged extension.");
    }

    let extension: Extension | undefined;
    try {
        extension = await registry.publish(options.extensionFile!, options.pat);
    } catch (err) {
        if (options.skipDuplicate && err.message.endsWith('is already published.')) {
            console.log(err.message + ' Skipping publish.');
            return;
        } else {
            throw err;
        }
    }
    if (extension.error) {
        throw new Error(extension.error);
    }

    const name = `${extension.namespace}.${extension.name}`;
    let description = `${name} v${extension.version}`;
    if (extension.targetPlatform !== 'universal') {
        description += `@${extension.targetPlatform}`;
    }

    console.log(`\ud83d\ude80  Published ${description}`);
    if (extension.warning) {
        console.log(`\n!!  ${extension.warning}`);
    }
}

interface PublishCommonOptions extends RegistryOptions {
    /**
     * Path to the vsix file to be published. Cannot be used together with `packagePath`.
     */
    extensionFile?: string;
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
    /**
     * Mark this package as a pre-release. Only valid with `packagePath`.
     */
    preRelease?: boolean;
    /**
     * Whether to fail silently if version already exists on the marketplace
     */
    skipDuplicate?: boolean;
}

// Interface used by top level CLI
export interface PublishOptions extends PublishCommonOptions {

    /**
     * Target architectures.
     */
    targets?: string[];

    /**
     * Paths to the extension to be packaged and published. Cannot be used together
     * with `extensionFile`.
     */
    packagePath?: string[];

    /**
     * Whether to do dependency detection via npm or yarn
     */
    dependencies?: boolean;
}

// Interface used internally by the doPublish method
interface InternalPublishOptions extends PublishCommonOptions {

    /**
     * Only one target for our internal command.
     * Target architecture.
     */
    target?: string;

    /**
     * Only one path for our internal command.
     * Path to the extension to be packaged and published. Cannot be used together
     * with `extensionFile`.
     */
    packagePath?: string;

    /**
     * Whether to do dependency detection via npm or yarn
     */
     dependencies?: boolean;
}

async function packageExtension(options: InternalPublishOptions, registry: Registry): Promise<void> {
    if (registry.requiresLicense) {
        await checkLicense(options.packagePath!);
    }

    options.extensionFile = await createTempFile({ postfix: '.vsix' });
    const packageOptions: IPackageOptions = {
        packagePath: options.extensionFile,
        target: options.target,
        cwd: options.packagePath,
        baseContentUrl: options.baseContentUrl,
        baseImagesUrl: options.baseImagesUrl,
        useYarn: options.yarn,
        dependencies: options.dependencies,
        preRelease: options.preRelease
    };
    await createVSIX(packageOptions);
}