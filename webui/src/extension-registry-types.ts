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
export function isSuccess(obj: any): obj is SuccessResult {
    return obj && typeof obj.success === 'string';
}

export interface ErrorResult {
    error: string;
}
export function isError(obj: any): obj is ErrorResult {
    return obj && typeof obj.error === 'string';
}

export interface SearchResult {
    offset: number;
    totalSize: number;
    extensions: ExtensionRaw[];
}

export interface ExtensionRaw {
    name: string;
    namespace: string;
    url: UrlString;
    files: { [id: string]: UrlString };
    displayName?: string;
    version?: string;
    averageRating?: number;
    downloadCount?: number;
    timestamp?: string;
    description?: string;
}

export const VERSION_ALIASES = ['latest', 'preview'];

export interface Extension extends ExtensionRaw {
    namespaceUrl: UrlString;
    reviewsUrl: UrlString;

    publishedBy: UserData;
    unrelatedPublisher: boolean;
    namespaceAccess: 'public' | 'restricted';

    allVersions: { [version: string]: UrlString };
    versionAlias: string[];
    preview?: boolean;

    engines?: string[];
    categories?: string[];
    tags?: string[];
    reviewCount: number;
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
    removeMembershipUrl: string;
    setMembershipRoleUrl: string;
}

export interface Namespace {
    name: string;
    extensions: { [key: string]: string };
    memberships: NamespaceMembership[];
    addMembershipUrl: string;
    getMembersUrl: string;
}

export enum MembershipRole {
    CONTRIBUTOR = 'contributor',
    OWNER = 'owner'
}
