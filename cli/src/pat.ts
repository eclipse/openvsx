/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
import { CreateNamespaceOptions } from './create-namespace-options';
import { PublishOptions } from './publish-options';
import { VerifyPatOptions } from './verify-pat-options';
import { Registry } from './registry';
import { getUserInput } from './util';
import { openDefaultStore } from './store';

export async function doVerifyPat(options: VerifyPatOptions) {
    const registry = new Registry(options);
    const namespace = options.namespace as string;
    const pat = options.pat as string;
    const result = await registry.verifyPat(namespace, pat);
    if (result.error) {
        throw new Error(result.error);
    }
    console.log(`\ud83d\ude80  PAT valid to publish at ${namespace}`);
}

export async function requestPAT(namespace: string, options: CreateNamespaceOptions | PublishOptions | VerifyPatOptions, verify: boolean = true): Promise<string> {
    const pat = await getUserInput(`Personal Access Token for namespace '${namespace}':`);
    if (verify) {
        await doVerifyPat({ ...options, namespace, pat });
    }

    return pat;
}

export async function getPAT(namespace: string, options: CreateNamespaceOptions | PublishOptions | VerifyPatOptions, verify: boolean = true): Promise<string> {
    if (options?.pat) {
        return options.pat;
    }

    const store = await openDefaultStore();
    let pat = store.get(namespace);
    if (pat) {
        return pat;
    }

    pat = await requestPAT(namespace, options, verify);
    await store.add(namespace, pat);

    return pat;
}