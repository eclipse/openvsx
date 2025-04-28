/********************************************************************************
 * Copyright (c) 2024 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import { openDefaultStore } from "./store";

export default async function logout(namespaceName: string) {
	if (!namespaceName) {
        throw new Error('Missing namespace name.');
    }

	const store = await openDefaultStore();
	if (!store.get(namespaceName)) {
		throw new Error(`Unknown namespace '${namespaceName}'.`);
	}

	await store.delete(namespaceName);
    console.log(`\ud83d\ude80  ${namespaceName} removed from the list of known namespaces`);
}