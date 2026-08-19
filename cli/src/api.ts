/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createVSIX as createVsceVSIX, IPackageOptions } from '@vscode/vsce';
import { checkLicense } from './check-license';
import { publishPackage } from './publish-package';
import { PublishCommonOptions } from './publish-options';
import { Extension, Registry } from './registry';
import { addEnvOptions, readManifest, validateManifest } from './util';

/**
 * Options of {@link publishVSIX}.
 */
export type PublishVSIXOptions = Omit<PublishCommonOptions, 'extensionFile'>;

/**
 * Options of {@link createVSIX}.
 */
export interface CreateVSIXOptions extends Omit<PublishCommonOptions, 'extensionFile'> {
    /**
     * The location of the extension to package. Defaults to the current working directory.
     */
    packagePath?: string;
    /**
     * Where to write the package. Defaults to `NAME-VERSION.vsix` in {@link packagePath}.
     */
    outputPath?: string;
    /**
     * Target architecture the package is built for.
     */
    target?: string;
    /**
     * Whether to detect dependencies via npm or yarn.
     */
    dependencies?: boolean;
}

/**
 * Publishes extensions that are packaged already.
 *
 * Unlike {@link publish}, this rejects as soon as one of the packages cannot be published, and it
 * never asks the user for input unless {@link PublishVSIXOptions.interactive} says so: a missing
 * access token is an error rather than a prompt.
 *
 * @param packagePath path of the `.vsix` file to publish, or several of them
 * @returns the published extensions, excluding the ones skipped as duplicates
 */
export async function publishVSIX(
    packagePath: string | string[],
    options: PublishVSIXOptions = {}
): Promise<Extension[]> {
    const packagePaths = typeof packagePath === 'string' ? [packagePath] : packagePath;
    // Work on a copy: the environment must not leak into the options object of the caller.
    const resolvedOptions = { interactive: false, ...options };
    addEnvOptions(resolvedOptions);

    const published: Extension[] = [];
    // Sequentially and failing fast: a caller of this API wants the first error, not a summary of
    // everything that went wrong.
    for (const extensionFile of packagePaths) {
        const extension = await publishPackage(extensionFile, resolvedOptions);
        if (extension) {
            published.push(extension);
        }
    }

    return published;
}

/**
 * Packages an extension without publishing it, using `vsce` under the hood.
 *
 * Note that this validates the license of the extension when the target registry requires one, so
 * the resulting package can be published to that registry. It never asks the user for input unless
 * {@link CreateVSIXOptions.interactive} says so.
 *
 * @returns the path of the packaged extension
 */
export async function createVSIX(options: CreateVSIXOptions = {}): Promise<string> {
    options = { interactive: false, ...options };
    const manifest = await readManifest(options.packagePath);
    validateManifest(manifest);

    if (new Registry(options).requiresLicense) {
        await checkLicense(options.packagePath ?? '.', options);
    }

    // vsce resolves a missing package path relative to its own cwd, so compute it here to be able to
    // report back where the package ended up.
    const outputPath = options.outputPath ?? defaultPackagePath(manifest.name, manifest.version, options);
    const packageOptions: IPackageOptions = {
        packagePath: outputPath,
        target: options.target,
        cwd: options.packagePath,
        baseContentUrl: options.baseContentUrl,
        baseImagesUrl: options.baseImagesUrl,
        useYarn: options.yarn,
        dependencies: options.dependencies,
        preRelease: options.preRelease,
        allowMissingRepository: options.allowMissingRepository,
        version: options.packageVersion
    };
    await createVsceVSIX(packageOptions);

    return outputPath;
}

function defaultPackagePath(name: string, version: string, options: CreateVSIXOptions): string {
    const fileName = `${name}-${options.packageVersion ?? version}.vsix`;
    return options.packagePath ? `${options.packagePath.replace(/[\\/]+$/, '')}/${fileName}` : fileName;
}
