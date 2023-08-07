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
    Extension, UserData, ExtensionCategory, ExtensionReviewList, PersonalAccessToken, SearchResult, NewReview,
    SuccessResult, ErrorResult, CsrfTokenJson, isError, Namespace, NamespaceDetails, MembershipRole, SortBy,
    SortOrder, UrlString, NamespaceMembershipList, PublisherInfo, SearchEntry
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

    getNamespaceDetails(abortController: AbortController, name: string): Promise<Readonly<NamespaceDetails>> {
        const endpoint = createAbsoluteURL([this.serverUrl, 'api', name, 'details']);
        return sendRequest({ abortController, endpoint });
    }

    async setNamespaceDetails(abortController: AbortController, details: NamespaceDetails): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        const endpoint = createAbsoluteURL([this.serverUrl, 'user', 'namespace', details.name, 'details']);
        return sendRequest({
            abortController,
            method: 'POST',
            payload: details,
            credentials: true,
            endpoint,
            headers
        });
    }

    search(abortController: AbortController, filter?: ExtensionFilter): Promise<Readonly<SearchResult | ErrorResult>> {
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
        return sendRequest({ abortController, endpoint });
    }

    getExtensionDetail(abortController: AbortController, extensionUrl: UrlString): Promise<Readonly<Extension | ErrorResult>> {
        return sendRequest({ abortController, endpoint: extensionUrl });
    }

    getExtensionReadme(abortController: AbortController, extension: Extension): Promise<string> {
        return sendRequest({
            abortController,
            endpoint: extension.files.readme!,
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
        });
    }

    getExtensionChangelog(abortController: AbortController, extension: Extension): Promise<string> {
        return sendRequest({
            abortController,
            endpoint: extension.files.changelog!,
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
        });
    }

    getExtensionIcon(abortController: AbortController, extension: Extension | SearchEntry): Promise<string | undefined> {
        if (!extension.files.icon) {
            return Promise.resolve(undefined);
        }

        return sendRequest({
            abortController,
            endpoint: extension.files.icon,
            headers: { 'Accept': 'application/octet-stream' },
            followRedirect: true
        }).then(value => {
            const blob = value as Blob;
            return URL.createObjectURL(blob);
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

    getExtensionReviews(abortController: AbortController, extension: Extension): Promise<Readonly<ExtensionReviewList>> {
        return sendRequest({ abortController, endpoint: extension.reviewsUrl });
    }

    async postReview(abortController: AbortController, review: NewReview, postReviewUrl: UrlString): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            method: 'POST',
            payload: review,
            credentials: true,
            endpoint: postReviewUrl,
            headers
        });
    }

    async deleteReview(abortController: AbortController, deleteReviewUrl: string): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: deleteReviewUrl,
            headers
        });
    }

    getUser(abortController: AbortController): Promise<Readonly<UserData | ErrorResult>> {
        return sendRequest({
            abortController,
            endpoint: createAbsoluteURL([this.serverUrl, 'user']),
            credentials: true
        });
    }

    getUserAuthError(abortController: AbortController): Promise<Readonly<ErrorResponse>> {
        return sendRequest({
            abortController,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'auth-error']),
            credentials: true
        });
    }

    getUserByName(abortController: AbortController, name: string): Promise<Readonly<UserData>[]> {
        return sendRequest({
            abortController,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'search', name]),
            credentials: true
        });
    }

    getAccessTokens(abortController: AbortController, user: UserData): Promise<Readonly<PersonalAccessToken>[]> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: user.tokensUrl
        });
    }

    async createAccessToken(abortController: AbortController, user: UserData, description: string): Promise<Readonly<PersonalAccessToken>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        const endpoint = addQuery(user.createTokenUrl, [{ key: 'description', value: description }]);
        return sendRequest({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint,
            headers
        });
    }

    async deleteAccessToken(abortController: AbortController, token: PersonalAccessToken): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: token.deleteTokenUrl,
            headers
        });
    }

    async deleteAllAccessTokens(abortController: AbortController, tokens: PersonalAccessToken[]): Promise<Readonly<SuccessResult | ErrorResult>[]> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return await Promise.all(tokens.map(token => sendRequest<SuccessResult | ErrorResult>({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: token.deleteTokenUrl,
            headers
        })));
    }

    getCsrfToken(abortController: AbortController): Promise<Readonly<CsrfTokenJson | ErrorResult>> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'csrf'])
        });
    }

    getNamespaces(abortController: AbortController): Promise<Readonly<Namespace>[]> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'namespaces'])
        });
    }

    getNamespaceMembers(abortController: AbortController, namespace: Namespace): Promise<Readonly<NamespaceMembershipList>> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: namespace.membersUrl
        });
    }

    async setNamespaceMember(abortController: AbortController, endpoint: UrlString, user: UserData, role: MembershipRole | 'remove'): Promise<Readonly<SuccessResult | ErrorResult>[]> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        const query = [
            { key: 'user', value: user.loginName },
            { key: 'provider', value: user.provider },
            { key: 'role', value: role }
        ];
        return sendRequest({
            abortController,
            headers,
            method: 'POST',
            credentials: true,
            endpoint: addQuery(endpoint, query)
        });
    }

    async signPublisherAgreement(abortController: AbortController): Promise<Readonly<UserData | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest<UserData | ErrorResult>({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'publisher-agreement']),
            headers
        });
    }

    getStaticContent(abortController: AbortController, url: string): Promise<string> {
        return sendRequest({
            abortController,
            endpoint: url,
            headers: { 'Accept': 'text/plain' },
            followRedirect: true
        });
    }

    async publishExtension(abortController: AbortController, extensionPackage: File): Promise<Readonly<Extension | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/octet-stream'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        return sendRequest<Extension | ErrorResult>({
            abortController,
            method: 'POST',
            credentials: true,
            payload: extensionPackage,
            headers: headers,
            endpoint: createAbsoluteURL([this.serverUrl, 'api', 'user', 'publish'])
        });
    }

    async createNamespace(abortController: AbortController, name: string): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        return sendRequest<SuccessResult | ErrorResult>({
            abortController,
            method: 'POST',
            credentials: true,
            payload: { name: name },
            headers: headers,
            endpoint: createAbsoluteURL([this.serverUrl, 'api', 'user', 'namespace', 'create'])
        });
    }

    async getExtensions(abortController: AbortController): Promise<Readonly<Extension[] | ErrorResult>> {
        const csrfResponse = await this.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        return sendRequest<Extension[] | ErrorResult>({
            abortController,
            method: 'GET',
            credentials: true,
            headers: headers,
            endpoint: createAbsoluteURL([this.serverUrl, 'user', 'extensions'])
        });
    }
}

export class AdminService {

    constructor(readonly registry: ExtensionRegistryService) { }

    getExtension(abortController: AbortController, namespace: string, extension: string): Promise<Readonly<Extension>> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'extension', namespace, extension])
        });
    }

    async deleteExtensions(abortController: AbortController, req: { namespace: string, extension: string, targetPlatformVersions?: object[] }): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.registry.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }

        return sendRequest({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'extension', req.namespace, req.extension, 'delete']),
            headers,
            payload: req.targetPlatformVersions
        });
    }

    getNamespace(abortController: AbortController, name: string): Promise<Readonly<Namespace>> {
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'namespace', name])
        });
    }

    async createNamespace(abortController: AbortController, namespace: { name: string }): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.registry.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'create-namespace']),
            method: 'POST',
            payload: namespace,
            headers
        });
    }

    async changeNamespace(abortController: AbortController, req: {oldNamespace: string, newNamespace: string, removeOldNamespace: boolean, mergeIfNewNamespaceAlreadyExists: boolean}): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.registry.getCsrfToken(abortController);
        const headers: Record<string, string> = {
            'Content-Type': 'application/json;charset=UTF-8'
        };
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'change-namespace']),
            method: 'POST',
            payload: req,
            headers
        });
    }

    async getPublisherInfo(abortController: AbortController, provider: string, login: string): Promise<Readonly<PublisherInfo>> {
        return sendRequest({
            abortController,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'publisher', provider, login]),
            credentials: true
        });
    }

    async revokePublisherContributions(abortController: AbortController, provider: string, login: string): Promise<Readonly<SuccessResult | ErrorResult>> {
        const csrfResponse = await this.registry.getCsrfToken(abortController);
        const headers: Record<string, string> = {};
        if (!isError(csrfResponse)) {
            const csrfToken = csrfResponse as CsrfTokenJson;
            headers[csrfToken.header] = csrfToken.value;
        }
        return sendRequest({
            abortController,
            method: 'POST',
            credentials: true,
            endpoint: createAbsoluteURL([this.registry.serverUrl, 'admin', 'publisher', provider, login, 'revoke']),
            headers
        });
    }

}

export interface ExtensionFilter {
    query: string;
    category: ExtensionCategory | '';
    size: number;
    offset: number;
    sortBy: SortBy;
    sortOrder: SortOrder;
}
