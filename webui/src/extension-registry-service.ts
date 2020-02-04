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
import { createAbsoluteURL, addQuery } from "./utils";
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
        const query: { key: string, value: string | number }[] = [];
        if (filter) {
            if (filter.query)
                query.push({ key: 'query', value: filter.query });
            if (filter.category)
                query.push({ key: 'category', value: filter.category });
            if (filter.offset)
                query.push({ key: 'offset', value: filter.offset });
            if (filter.size)
                query.push({ key: 'size', value: filter.size });
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

    getUser(): Promise<UserData | ErrorResult> {
        return sendRequest({
            endpoint: createAbsoluteURL([this.serverUrl, 'user']),
            credentials: true
        });
    }

    getTokens(tokensUrl: string): Promise<PersonalAccessToken[]> {
        return sendRequest({ endpoint: tokensUrl });
    }

    createToken(createTokenUrl: string, description: string): Promise<PersonalAccessToken> {
        const endpoint = addQuery(createTokenUrl, [{ key: 'description', value: description }]);
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint
        });
    }

    deleteToken(deleteTokenUrl: string, token: PersonalAccessToken): Promise<{} | ErrorResult> {
        const endpoint = addQuery(deleteTokenUrl, [{ key: 'token', value: token.value }]);
        return sendRequest({
            method: 'DELETE',
            credentials: true,
            endpoint
        });
    }

}

export interface ExtensionFilter {
    query?: string;
    category?: ExtensionCategory | '';
    size?: number;
    offset?: number;
}
