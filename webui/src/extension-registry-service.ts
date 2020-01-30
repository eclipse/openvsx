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
    ExtensionFilter, Extension, UserData, ExtensionCategory,
    ExtensionReviewList, PersonalAccessToken, SearchResult, NewReview, ExtensionRaw
} from "./extension-registry-types";
import { createAbsoluteURL } from "./utils";
import { getTokens, generateToken, deleteToken, deleteTokens } from "./mock-token-api";
import { sendRequest } from "./server-request";

export class ExtensionRegistryService {

    constructor(protected readonly serverUrl: string) {}

    getLoginUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'oauth2', 'authorization', 'github']);
    }

    getLogoutUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'logout']);
    }

    getExtensionApiUrl(extension: ExtensionRaw) {
        return createAbsoluteURL([this.serverUrl, 'api', extension.publisher, extension.name]);
    }


    search(filter?: ExtensionFilter): Promise<SearchResult> {
        let query: { key: string, value: string | number }[] | undefined;
        if (filter) {
            query = [];
            for (const key in filter) {
                const value = filter[key];
                if (value) {
                    query.push({ key, value });
                }
            }
        }
        const endpoint = createAbsoluteURL([this.serverUrl, 'api', '-', 'search'], query);
        return sendRequest<SearchResult>({ endpoint });
    }

    getExtensionDetail(extensionURL: string): Promise<Extension> {
        return sendRequest<Extension>({ endpoint: extensionURL });
    }

    getExtensionReadme(readmeUrl: string): Promise<string> {
        return sendRequest<string>({
            endpoint: readmeUrl,
            operation: response => response.text()
        });
    }

    getExtensionReviews(reviewsUrl: string): Promise<ExtensionReviewList> {
        return sendRequest<ExtensionReviewList>({ endpoint: reviewsUrl });
    }

    postReview(review: NewReview, postUrl: string): Promise<void> {
        return sendRequest({
            method: 'POST',
            payload: review,
            contentType: 'application/json;charset=UTF-8',
            credentials: true,
            endpoint: postUrl
        });
    }

    async getUser(): Promise<UserData | undefined> {
        try {
            const user = await sendRequest({
                endpoint: createAbsoluteURL([this.serverUrl, 'user']),
                credentials: true
            });
            if (UserData.is(user)) {
                return user;
            }
        } catch (err) {
            console.warn(err);
        }
        return undefined;
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
