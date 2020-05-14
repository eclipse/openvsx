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
    readonly success: string;
}
export function isSuccess(obj: any): obj is SuccessResult {
    return obj && typeof obj.success === 'string';
}

export interface ErrorResult {
    readonly error: string;
}
export function isError(obj: any): obj is ErrorResult {
    return obj && typeof obj.error === 'string';
}

export interface SearchResult {
    readonly offset: number;
    readonly totalSize: number;
    readonly extensions: ExtensionRaw[];
}

export interface ExtensionRaw {
    readonly name: string;
    readonly namespace: string;
    readonly url: UrlString;
    readonly files: { [id: string]: UrlString };
    readonly displayName?: string;
    readonly version?: string;
    readonly averageRating?: number;
    readonly downloadCount?: number;
    readonly timestamp?: string;
    readonly description?: string;
}

export interface Extension extends ExtensionRaw {
    readonly namespaceUrl: UrlString;
    readonly reviewsUrl: UrlString;

    readonly publishedBy: UserData;
    readonly unrelatedPublisher: boolean;
    readonly namespaceAccess: 'public' | 'restricted';

    readonly allVersions: { [version: string]: UrlString };
    readonly preview?: boolean;

    readonly engines?: string[];
    readonly categories?: string[];
    readonly tags?: string[];
    readonly reviewCount: number;
    readonly license?: string;
    readonly homepage?: string;
    readonly repository?: string;
    readonly bugs?: string;
    readonly markdown?: 'github' | 'standard';
    readonly galleryColor?: string;
    readonly galleryTheme?: 'light' | 'dark';
    readonly qna?: UrlString | 'marketplace' | 'false';
    readonly badges?: Badge[];
    readonly dependencies?: ExtensionReference[];
    readonly bundledExtensions?: ExtensionReference[];
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
    extensions: Extension[];
    memberships: NamespaceMembership[];
    addMembershipUrl: string;
    getMembersUrl: string;
}

export enum MembershipRole {
    CONTRIBUTOR = 'contributor',
    OWNER = 'owner'
}
