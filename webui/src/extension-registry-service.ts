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
    Extension, UserData, ExtensionCategory, ExtensionReviewList, PersonalAccessToken,
    SearchResult, NewReview, ExtensionRaw, ErrorResult
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

    search(filter?: ExtensionFilter): Promise<SearchResult | ErrorResult> {
        let query: { key: string, value: string | number }[] | undefined;
        if (filter) {
            query = Object.getOwnPropertyNames(filter)
                .map(key => ({ key, value: filter[key] }))
                .filter(({value}) => value !== undefined) as { key: string, value: string | number }[];
        }
        const endpoint = createAbsoluteURL([this.serverUrl, 'api', '-', 'search'], query);
        return sendRequest({ endpoint });
    }

    getExtensionDetail(extensionUrl: string): Promise<Extension | ErrorResult> {
        return sendRequest({ endpoint: extensionUrl });
    }

    getExtensionReadme(readmeUrl: string): Promise<string> {
        return sendRequest({
            endpoint: readmeUrl,
            accept: 'text/plain'
        });
    }

    getExtensionReviews(reviewsUrl: string): Promise<ExtensionReviewList> {
        return sendRequest({ endpoint: reviewsUrl });
    }

    postReview(review: NewReview, postUrl: string): Promise<{} | ErrorResult> {
        return sendRequest({
            method: 'POST',
            payload: review,
            contentType: 'application/json;charset=UTF-8',
            credentials: true,
            endpoint: postUrl
        });
    }

    async getUser(): Promise<UserData | ErrorResult> {
        return await sendRequest({
            endpoint: createAbsoluteURL([this.serverUrl, 'user']),
            credentials: true
        });
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

export interface ExtensionFilter {
    query?: string;
    category?: ExtensionCategory;
    size?: number;
    offset?: number;
    [key: string]: string | number | undefined;
}
