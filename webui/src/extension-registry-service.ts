/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import {
    ExtensionFilter, Extension, ExtensionReview, UserData, ExtensionCategory,
    ExtensionReviewList, PersonalAccessToken, SearchResult
} from "./extension-registry-types";
import { createAbsoluteURL } from "./utils";
import { getExtensions, getExtension, getExtensionReadme, getExtensionReviews, postReview, getUser } from "./extension-registry-api";
import { getTokens, generateToken, deleteToken, deleteTokens } from "./mock-token-api";

export class ExtensionRegistryService {
    private static _instance: ExtensionRegistryService;

    static get instance(): ExtensionRegistryService {
        if (!ExtensionRegistryService._instance) {
            ExtensionRegistryService._instance = new ExtensionRegistryService();
        }
        return ExtensionRegistryService._instance;
    }

    serverUrl: string;

    getLoginUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'oauth2', 'authorization', 'github']);
    }

    getLogoutUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'logout']);
    }

    getExtensions(filter?: ExtensionFilter): Promise<SearchResult> {
        let query: { key: string, value: string | number }[] | undefined;
        if (filter) {
            query = [];
            for (const key in filter) {
                if (filter[key]) {
                    const value = filter[key];
                    if (!!value) {
                        query.push({ key, value });
                    }
                }
            }
        }
        const endpoint = createAbsoluteURL([this.serverUrl, 'api', '-', 'search'], query);
        return getExtensions(endpoint);
    }

    getExtensionDetail(extensionURL: string): Promise<Extension> {
        return getExtension(extensionURL);
    }

    getExtensionReadme(readMeUrl: string): Promise<string> {
        return getExtensionReadme(readMeUrl);
    }

    getExtensionReviews(reviewsUrl: string): Promise<ExtensionReviewList> {
        return getExtensionReviews(reviewsUrl);
    }

    postReview(rating: ExtensionReview, postUrl: string): Promise<void> {
        return postReview(rating, postUrl);
    }

    async getUser(): Promise<UserData | undefined> {
        try {
            const user = await getUser(createAbsoluteURL([this.serverUrl, 'user']));
            if (UserData.is(user)) {
                return user;
            }
        } catch (err) {
            console.warn(err);
        }
        return;
    }

    getCategories(): ExtensionCategory[] {
        return [
            'Programming Languages',
            'Snippets',
            'Linters',
            'Themes',
            'Debuggers',
            'Formatters',
            'Keymaps',
            'SCM Providers',
            'Other',
            'Extension Packs',
            'Language Packs'
        ];
    }

    // TOKENS

    async getTokens(): Promise<PersonalAccessToken[]> {
        const tokens = await getTokens();
        const tArr: PersonalAccessToken[] = [];
        for (const id in tokens) {
            if (tokens[id]) {
                tArr.push(tokens[id]);
            }
        }
        return tArr;
    }

    generateToken(description: string): Promise<PersonalAccessToken> {
        return generateToken(description);
    }

    deleteToken(tokenId: string): Promise<void> {
        return deleteToken(tokenId);
    }

    deleteTokens(): Promise<void> {
        return deleteTokens();
    }
}