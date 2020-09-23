/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

export type UrlString = string;

export interface SuccessResult {
    success: string;
}
export function isSuccess(obj: unknown): obj is SuccessResult {
    return obj && typeof (obj as any).success === 'string';
}

export interface ErrorResult {
    error: string;
}
export function isError(obj: unknown): obj is ErrorResult {
    return obj && typeof (obj as any).error === 'string';
}

export interface SearchResult {
    offset: number;
    totalSize: number;
    extensions: SearchEntry[];
}

export interface SearchEntry {
    url: UrlString;
    // key: file type, value: url
    files: { [id: string]: UrlString };
    name: string;
    namespace: string;
    version: string;
    timestamp?: string;
    allVersions: {
        url: UrlString;
        // key: file type, value: url
        files: { [id: string]: UrlString };
        version: string;
        // key: engine, value: version constraint
        engines?: { [engine: string]: string };
    }[];
    averageRating?: number;
    downloadCount?: number;
    displayName?: string;
    description?: string;
}

export const VERSION_ALIASES = ['latest', 'preview'];

export interface Extension {
    namespaceUrl: UrlString;
    reviewsUrl: UrlString;
    // key: file type, value: url
    files: { [id: string]: UrlString };

    name: string;
    namespace: string;
    version: string;
    publishedBy: UserData;
    unrelatedPublisher: boolean;
    namespaceAccess: 'public' | 'restricted';
    // key: version, value: url
    allVersions: { [version: string]: UrlString };

    averageRating?: number;
    downloadCount: number;
    reviewCount: number;

    versionAlias: string[];
    timestamp: string;
    preview?: boolean;
    displayName?: string;
    description?: string;

    // key: engine, value: version constraint
    engines?: string[];
    categories?: string[];
    tags?: string[];
    license?: string;
    homepage?: string;
    repository?: string;
    bugs?: string;
    markdown?: 'github' | 'standard';
    galleryColor?: string;
    galleryTheme?: 'light' | 'dark';
    qna?: UrlString | 'marketplace' | 'false';
    badges?: Badge[];
    dependencies?: ExtensionReference[];
    bundledExtensions?: ExtensionReference[];
}

export interface Badge {
    url: UrlString;
    href: UrlString;
    description: string;
}

export interface ExtensionReference {
    namespace: string;
    extension: string;
    version?: string;
}

export type StarRating = 1 | 2 | 3 | 4 | 5;
export interface NewReview {
    rating: StarRating;
    title?: string;
    comment: string;
}

export interface ExtensionReview extends NewReview {
    user: UserData;
    timestamp: string;
}

export interface ExtensionReviewList {
    postUrl: UrlString;
    deleteUrl: UrlString;
    reviews: ExtensionReview[];
}

export interface UserData {
    loginName: string;
    tokensUrl: UrlString;
    createTokenUrl: UrlString;
    fullName?: string;
    avatarUrl?: UrlString;
    homepage?: string;
    provider?: string;
    role?: string;
}

export function isEqualUser(u1: UserData, u2: UserData): boolean {
    return u1.loginName === u2.loginName;
}

export interface PersonalAccessToken {
    id: number;
    value?: string;
    createdTimestamp: string;
    accessedTimestamp?: string;
    description: string;
    deleteTokenUrl: UrlString;
}

export type ExtensionCategory =
    'Programming Languages' |
    'Snippets' |
    'Linters' |
    'Themes' |
    'Debuggers' |
    'Formatters' |
    'Keymaps' |
    'SCM Providers' |
    'Other' |
    'Extension Packs' |
    'Language Packs';

export interface CsrfTokenJson {
    value: string;
    header: string;
}

export interface NamespaceMembership {
    namespace: string;
    role: MembershipRole;
    user: UserData;
}

export interface Namespace {
    name: string;
    extensions: { [key: string]: string };
    access: 'public' | 'restricted';
    membersUrl: UrlString;
    roleUrl: UrlString;
}

export type MembershipRole = 'contributor' | 'owner';
export type SortBy = 'relevance' | 'timestamp' | 'averageRating' | 'downloadCount';
export type SortOrder = 'asc' | 'desc';
