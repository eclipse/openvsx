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
    SearchResult, NewReview, SuccessResult, ErrorResult, CsrfTokenJson, isError, Namespace, MembershipRole, SortBy, SortOrder, UrlString, NamespaceMembershipList, PublisherInfo
} from './extension-registry-types';
import { createAbsoluteURL, addQuery } from './utils';
import { sendRequest, ErrorResponse } from './server-request';

export class ExtensionRegistryService {

    readonly admin: AdminService;

    constructor(readonly serverUrl: string = '', admin?: AdminService) {
        this.admin = admin ?? new AdminService(this);
    }

    getLoginUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'oauth2', 'authorization', 'github']);
    }

    getLogoutUrl(): string {
        return createAbsoluteURL([this.serverUrl, 'logout']);
    }

    getExtensionApiUrl(ext: { namespace: string, name: string, target?: string, version?: string }): string {
        const arr = [this.serverUrl, 'api', ext.namespace, ext.name];
        if (ext.target) {
            arr.push(ext.target);
        }
        if (ext.version) {
            arr.push(ext.version);
        }

        return createAbsoluteURL(arr);
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
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
        });
    }

    getExtensionChangelog(extension: Extension): Promise<string> {
        return sendRequest({
            endpoint: extension.files.changelog!,
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
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
            'Language Packs',
            'Data Science',
            'Machine Learning',
            'Visualization',
            'Notebooks'
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

    getUserAuthError(): Promise<Readonly<ErrorResponse>> {
        return sendRequest({
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'auth-error']),
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

    getNamespaceMembers(namespace: Namespace): Promise<Readonly<NamespaceMembershipList>> {
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
        return sendRequest({
            headers,
            method: 'POST',
            credentials: true,
            endpoint: addQuery(endpoint, query)
        });
    }

    async signPublisherAgreement(): Promise<Readonly<UserData | ErrorResult>> {
        const csrfToken = await this.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest<UserData | ErrorResult>({
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'publisher-agreement']),
            headers
        });
    }

    getStaticContent(url: string): Promise<string> {
        return sendRequest({
            endpoint: url,
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
        });
    }
}

export class AdminService {

    constructor(readonly registry: ExtensionRegistryService) { }

    getExtension(namespace: string, extension: string): Promise<Readonly<Extension>> {
        return sendRequest({
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'extension', namespace, extension])
        });
    }

    async deleteExtensions(req: { namespace: string, extension: string, targetPlatformVersions?: object[] }): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.registry.getCsrfToken();
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'extension', req.namespace, req.extension, 'delete']),
            headers,
            payload: req.targetPlatformVersions
        });
    }

    getNamespace(name: string): Promise<Readonly<Namespace>> {
        return sendRequest({
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'namespace', name])
        });
    }

    async createNamespace(namespace: { name: string }): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.registry.getCsrfToken();
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'create-namespace']),
            method: 'POST',
            payload: namespace,
            headers
        });
    }

    async getPublisherInfo(provider: string, login: string): Promise<Readonly<PublisherInfo>> {
        return sendRequest({
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'publisher', provider, login]),
            credentials: true
        });
    }

    async revokePublisherContributions(provider: string, login: string): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfToken = await this.registry.getCsrfToken();
        const headers: Record<string, string> = {};
        if (!isError(csrfToken)) {
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'publisher', provider, login, 'revoke']),
            headers
        });
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
