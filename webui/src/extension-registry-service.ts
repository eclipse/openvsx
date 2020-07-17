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
    SearchResult, NewReview, SuccessResult, ErrorResult, CsrfTokenJson, isError, Namespace, NamespaceMembership, MembershipRole, SortBy, SortOrder, UrlString
} from './extension-registry-types';
import { createAbsoluteURL, addQuery } from './utils';
import { sendRequest } from './server-request';

export class ExtensionRegistryService {

    constructor(protected readonly serverUrl: string = '') { }

    getLoginUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'oauth2', 'authorization', 'github']);
    }

    getLogoutUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'logout']);
    }

    getExtensionApiUrl(ext: { namespace: string, name: string, version?: string }): string {
        if (ext.version) {
            return createAbsoluteURL([this.serverUrl, 'api', ext.namespace, ext.name, ext.version]);
        } else {
            return createAbsoluteURL([this.serverUrl, 'api', ext.namespace, ext.name]);
        }
    }

    search(filter?: ExtensionFilter): Promise<Readonly<SearchResult | ErrorResult>> {
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
            if (filter.sortBy)
                query.push({ key: 'sortBy', value: filter.sortBy });
            if (filter.sortOrder)
                query.push({ key: 'sortOrder', value: filter.sortOrder });
        }
        const endpoint = createAbsoluteURL([this.serverUrl, 'api', '-', 'search'], query);
        return sendRequest({ endpoint });
    }

    getExtensionDetail(extensionUrl: UrlString): Promise<Readonly<Extension | ErrorResult>> {
        return sendRequest({ endpoint: extensionUrl });
    }

    getExtensionReadme(extension: Extension): Promise<string> {
        return sendRequest({
            endpoint: extension.files.readme!,
            headers: { 'Accept': 'text/plain' }
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

    getExtensionReviews(extension: Extension): Promise<Readonly<ExtensionReviewList>> {
        return sendRequest({ endpoint: extension.reviewsUrl });
    }

    async postReview(review: NewReview, postReviewUrl: UrlString): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            method: 'POST',
            payload: review,
            credentials: true,
            endpoint: postReviewUrl,
            headers
        });
    }

    async deleteReview(deleteReviewUrl: string): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint: deleteReviewUrl,
            headers
        });
    }

    getUser(): Promise<Readonly<UserData | ErrorResult>> {
        return sendRequest({
            endpoint: createAbsoluteURL([this.serverUrl, 'user']),
            credentials: true
        });
    }

    getUserByName(name: string): Promise<Readonly<UserData>[]> {
        return sendRequest({
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'search', name]),
            credentials: true
        });
    }

    getAccessTokens(user: UserData): Promise<Readonly<PersonalAccessToken>[]> {
        return sendRequest({
            credentials: true,
            endpoint: user.tokensUrl
        });
    }

    async createAccessToken(user: UserData, description: string): Promise<Readonly<PersonalAccessToken>> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        const endpoint = addQuery(user.createTokenUrl, [{ key: 'description', value: description }]);
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint,
            headers
        });
    }

    async deleteAccessToken(token: PersonalAccessToken): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint: token.deleteTokenUrl,
            headers
        });
    }

    async deleteAllAccessTokens(tokens: PersonalAccessToken[]): Promise<Readonly<SuccessResult | ErrorResult>[]> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return await Promise.all(tokens.map(token => sendRequest<SuccessResult | ErrorResult>({
            method: 'POST',
            credentials: true,
            endpoint: token.deleteTokenUrl,
            headers
        })));
    }

    getCsrfToken(): Promise<Readonly<CsrfTokenJson | ErrorResult>> {
        return sendRequest({
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'csrf'])
        });
    }

    getNamespaces(): Promise<Readonly<Namespace>[]> {
        return sendRequest({
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'namespaces'])
        });
    }

    getNamespaceMembers(namespace: Namespace): Promise<Readonly<NamespaceMembership>[]> {
        return sendRequest({
            credentials: true,
            endpoint: namespace.membersUrl
        });
    }

    async setNamespaceMember(endpoint: UrlString, user: UserData, role: MembershipRole | 'remove'): Promise<Readonly<SuccessResult | ErrorResult>[]> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        const query = [
            { key: 'user', value: user.loginName },
            { key: 'provider', value: user.provider },
            { key: 'role', value: role }
        ];
        return sendRequest(
            {
                headers,
                method: 'POST',
                credentials: true,
                endpoint: addQuery(endpoint, query)
            }
        );
    }

}

export interface ExtensionFilter {
    query?: string;
    category?: ExtensionCategory | '';
    size?: number;
    offset?: number;
    sortBy?: SortBy;
    sortOrder?: SortOrder;
}
