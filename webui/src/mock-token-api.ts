/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

 import { PersonalAccessToken } from "./extension-registry-types";

export type ExtensionRegistryTokenStore = { [key: string]: PersonalAccessToken };
export let tokenStore: ExtensionRegistryTokenStore = {};

let count = 0;

export async function getTokens(): Promise<ExtensionRegistryTokenStore> {
    return tokenStore;
}

export async function generateToken(description: string): Promise<PersonalAccessToken> {
    const token: PersonalAccessToken = {
        description,
        id: 't' + count,
        tokenValue: `Token ${count}: Some token which was generated for some app`,
        userId: 'TEST'
    };
    tokenStore[token.id] = token;
    count++;
    return tokenStore['t' + (count - 1)];
}

export async function deleteToken(tokenId: string): Promise<void> {
    delete tokenStore[tokenId];
}

export async function deleteTokens(): Promise<void> {
    tokenStore = {};
}
