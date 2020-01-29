/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

 import { PersonalAccessToken } from "../extension-registry-types";

export type ExtensionRegistryTokenStore = { [key: string]: PersonalAccessToken };
export let tokenStore: ExtensionRegistryTokenStore = {};

export class MockTokenAPI {

    protected count = 0;

    async getTokens(): Promise<ExtensionRegistryTokenStore> {
        return tokenStore;
    }

    async generateToken(description: string): Promise<PersonalAccessToken> {
        const token: PersonalAccessToken = {
            description,
            id: 't' + this.count,
            tokenValue: `Token ${this.count}: Some token which was generated for some app`,
            userId: 'TEST'
        };
        tokenStore[token.id] = token;
        this.count++;
        return tokenStore['t' + (this.count - 1)];
    }

    async deleteToken(tokenId: string): Promise<void> {
        delete tokenStore[tokenId];
    }

    async deleteTokens(): Promise<void> {
        tokenStore = {};
    }
}