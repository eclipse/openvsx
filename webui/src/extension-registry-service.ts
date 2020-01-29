/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { ExtensionRegistryAPI } from "./extension-registry-api";
import {
    ExtensionFilter, Extension, ExtensionReview, UserData, ExtensionCategory,
    ExtensionReviewList, PersonalAccessToken, SearchResult
} from "./extension-registry-types";
import { createAbsoluteURL } from "./utils";
import { MockTokenAPI } from "./pages/mock-token-api";

export class ExtensionRegistryService {
    private static _instance: ExtensionRegistryService;
    private api: ExtensionRegistryAPI;
    private _apiUrl: string;

    private tokenApiMock: MockTokenAPI;

    private constructor() {
        this.api = new ExtensionRegistryAPI();

        this.tokenApiMock = new MockTokenAPI();
    }

    static get instance(): ExtensionRegistryService {
        if (!ExtensionRegistryService._instance) {
            ExtensionRegistryService._instance = new ExtensionRegistryService();
        }
        return ExtensionRegistryService._instance;
    }

    set apiUrl(url: string) {
        this._apiUrl = url;
    }

    get apiUrl(): string {
        return this._apiUrl;
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
        const endpoint = createAbsoluteURL([this._apiUrl, '-', 'search'], query);
        return this.api.getExtensions(endpoint);
    }

    getExtensionDetail(extensionURL: string): Promise<Extension> {
        return this.api.getExtension(extensionURL);
    }

    getExtensionReadme(readMeUrl: string): Promise<string> {
        return this.api.getExtensionReadme(readMeUrl);
    }

    getExtensionReviews(reviewsUrl: string): Promise<ExtensionReviewList> {
        return this.api.getExtensionReviews(reviewsUrl);
    }

    postReview(rating: ExtensionReview, postUrl: string): Promise<void> {
        return this.api.postReview(rating, postUrl);
    }

    async getUser(): Promise<UserData | undefined> {
        try {
            const user = await this.api.getUser(createAbsoluteURL([this._apiUrl, '-', 'user']));
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
        const tokens = await this.tokenApiMock.getTokens();
        const tArr: PersonalAccessToken[] = [];
        for (const id in tokens) {
            if (tokens[id]) {
                tArr.push(tokens[id]);
            }
        }
        return tArr;
    }

    generateToken(description: string): Promise<PersonalAccessToken> {
        return this.tokenApiMock.generateToken(description);
    }

    deleteToken(tokenId: string): Promise<void> {
        return this.tokenApiMock.deleteToken(tokenId);
    }

    deleteTokens(): Promise<void> {
        return this.tokenApiMock.deleteTokens();
    }
}