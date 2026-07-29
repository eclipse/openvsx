/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { consoleLogger } from './logger';
import { getPAT } from './pat';
import { PublishCommonOptions } from './publish-options';
import { Extension, Registry } from './registry';
import { readVSIXPackage } from './zip';

/**
 * Publishes a single packaged extension, the step `publish` and `publishVSIX` share.
 *
 * Resolves with the published extension, or with `undefined` if the version existed already and
 * {@link PublishCommonOptions.skipDuplicate} is set.
 */
export async function publishPackage(
    extensionFile: string,
    options: PublishCommonOptions = {}
): Promise<Extension | undefined> {
    const log = options.log ?? consoleLogger;
    const registry = new Registry(options);
    let pat = options.pat;
    if (!pat) {
        const namespace = (await readVSIXPackage(extensionFile)).publisher;
        pat = await getPAT(namespace, options);
    }

    let extension: Extension;
    try {
        extension = await registry.publish(extensionFile, pat);
    } catch (err) {
        if (options.skipDuplicate && err.message.endsWith('is already published.')) {
            log.log(err.message + ' Skipping publish.');
            return undefined;
        } else {
            throw err;
        }
    }
    if (extension.error) {
        throw new Error(extension.error);
    }

    let description = `${extension.namespace}.${extension.name} v${extension.version}`;
    if (extension.targetPlatform !== 'universal') {
        description += `@${extension.targetPlatform}`;
    }

    log.log(`\ud83d\ude80  Published ${description}`);
    if (extension.warning) {
        log.log(`\n!!  ${extension.warning}`);
    }

    return extension;
}
