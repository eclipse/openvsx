/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import { createVSIX } from './api';
import { publishPackage } from './publish-package';
import { createTempFile, addEnvOptions } from './util';
import { Extension } from './registry';
import { PublishOptions, PublishCommonOptions } from './publish-options';
import { consoleLogger } from './logger';

/**
 * Publishes an extension, packaging it first if necessary.
 *
 * Every combination of package path and target is published independently, so the returned array
 * reports the outcome of each one. Use {@link publishVSIX} to have the first failure reject instead.
 */
export async function publish(options: PublishOptions = {}): Promise<PromiseSettledResult<Extension | undefined>[]> {
    // Work on a copy: the environment must not leak into the options object of the caller. Prompting
    // stays allowed here, unlike in the programmatic API.
    const resolvedOptions = { interactive: true, ...options };
    addEnvOptions(resolvedOptions);
    const internalPublishOptions: InternalPublishOptions[] = [];
    const packagePaths = resolvedOptions.packagePath || [undefined];
    const targets = resolvedOptions.targets || [undefined];
    for (const packagePath of packagePaths) {
        for (const target of targets) {
            internalPublishOptions.push({ ...resolvedOptions, packagePath: packagePath, target: target });
        }
    }

    return Promise.allSettled(internalPublishOptions.map(publishOptions => doPublish(publishOptions)));
}

/**
 * Publishes a single extension, packaging it first if necessary. Resolves with the published
 * extension, or with `undefined` if the version existed already and `skipDuplicate` is set.
 */
async function doPublish(options: InternalPublishOptions = {}): Promise<Extension | undefined> {
    const log = options.log ?? consoleLogger;
    // if the packagePath is a link to a vsix, don't need to package it
    if (options.packagePath?.endsWith('.vsix')) {
        options.extensionFile = options.packagePath;
        delete options.packagePath;
        delete options.target;
    }

    if (!options.extensionFile) {
        // Package into a temporary file instead of the location vsce would pick, so that publishing
        // does not leave a package behind in the extension directory.
        options.extensionFile = await createVSIX({
            ...options,
            outputPath: await createTempFile({ postfix: '.vsix' })
        });
        log.log(); // new line
    } else if (options.preRelease) {
        log.warn("Ignoring option '--pre-release' for prepackaged extension.");
    }

    return publishPackage(options.extensionFile, options);
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
