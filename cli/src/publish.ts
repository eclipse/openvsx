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
import { getPAT } from './pat';
import { createTempFile, addEnvOptions } from './util';
import { Extension, Registry } from './registry';
import { checkLicense } from './check-license';
import { readVSIXPackage } from './zip';
import { PublishOptions, PublishCommonOptions } from './publish-options';

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
            internalPublishOptions.push({ ...options, packagePath: packagePath, target: target });
        }
    }

    return Promise.allSettled(internalPublishOptions.map(publishOptions => doPublish(publishOptions)));
}

async function doPublish(options: InternalPublishOptions = {}): Promise<void> {
    // if the packagePath is a link to a vsix, don't need to package it
    if (options.packagePath?.endsWith('.vsix')) {
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

    if (!options.pat) {
        const namespace = (await readVSIXPackage(options.extensionFile!)).publisher;
        options.pat = await getPAT(namespace, options);
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
        preRelease: options.preRelease,
        version: options.packageVersion
    };
    await createVSIX(packageOptions);
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