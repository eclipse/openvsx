/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import {  } from '@vscode/vsce';
import { addEnvOptions, getUserInput } from './util';
import { openDefaultStore } from './store';
import { CreateNamespaceOptions } from './create-namespace-options';
import { PublishOptions } from './publish-options';
import { doVerifyPat } from './verify-pat';
import { VerifyPatOptions } from './verify-pat-options';
import { LoginOptions } from './login-options';

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

export default async function login(options: LoginOptions) {
	addEnvOptions(options);
	if (!options.namespace) {
        throw new Error('Missing namespace name.');
    }

	const store = await openDefaultStore();
	let pat = store.get(options.namespace);
	if (pat) {
		console.log(`Namespace '${options.namespace}' is already known.`);
		const answer = await getUserInput('Do you want to overwrite its PAT? [y/N] ');

		if (!/^y$/i.test(answer)) {
			throw new Error('Aborted.');
		}
	}

	pat = await requestPAT(options.namespace, options);
	await store.add(options.namespace, pat);
}