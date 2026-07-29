/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import { password } from '@inquirer/prompts';
import { CreateNamespaceOptions } from './create-namespace-options';
import { PublishOptions } from './publish-options';
import { VerifyPatOptions } from './verify-pat-options';
import { Registry } from './registry';
import { openDefaultStore } from './store';
import { consoleLogger } from './logger';

export async function doVerifyPat(options: VerifyPatOptions) {
    const registry = new Registry(options);
    const namespace = options.namespace as string;
    const pat = options.pat as string;
    const result = await registry.verifyPat(namespace, pat);
    if (result.error) {
        throw new Error(result.error);
    }
    (options.log ?? consoleLogger).log(`\ud83d\ude80  PAT valid to publish at ${namespace}`);
}

export async function requestPAT(namespace: string, options: CreateNamespaceOptions | PublishOptions | VerifyPatOptions, verify: boolean = true): Promise<string> {
    if (options.interactive === false) {
        throw new Error(`Cannot ask for the personal access token of namespace '${namespace}' without user interaction.`);
    }

    const pat = await password({
        message: `Personal Access Token for namespace '${namespace}':`,
        mask: true,
        validate: value => value.trim().length > 0 || 'A personal access token is required.'
    });
    if (verify) {
        await doVerifyPat({ ...options, namespace, pat });
    }

    return pat;
}

export async function getPAT(namespace: string, options: CreateNamespaceOptions | PublishOptions | VerifyPatOptions, verify: boolean = true): Promise<string> {
    if (options?.pat) {
        return options.pat;
    }

    const store = await openDefaultStore(options.log ?? consoleLogger);
    let pat = await store.get(namespace);
    if (pat) {
        return pat;
    }

    if (options.interactive === false) {
        throw new Error(
            `No personal access token found for namespace '${namespace}'.`
            + ` Pass the 'pat' option, set the OVSX_PAT environment variable`
            + ` or run 'ovsx login ${namespace}' to store one.`);
    }

    pat = await requestPAT(namespace, options, verify);
    await store.add(namespace, pat);

    return pat;
}
