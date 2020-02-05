/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

export interface ErrorResult {
    readonly error?: string;
}
export function isError(obj: any): obj is ErrorResult {
    return obj && typeof obj.error === 'string';
}

export interface SearchResult {
    readonly offset: number;
    readonly extensions: ExtensionRaw[];
}

export interface ExtensionRaw {
    readonly name: string;
    readonly publisher: string;
    readonly url: string;
    readonly iconUrl?: string;
    readonly displayName?: string;
    readonly version?: string;
    readonly averageRating?: number;
    readonly timestamp?: string;
    readonly description?: string;
}

export interface Extension extends ExtensionRaw {
    readonly publisherUrl: string;
    readonly reviewsUrl: string;
    readonly downloadUrl: string;

    readonly readmeUrl?: string;

    readonly allVersions: { [key: string]: string };
    readonly preview?: boolean;

    readonly categories?: string[];

    readonly reviewCount: number;

    readonly tags?: string[];
    readonly license?: string;
    readonly homepage?: string;
    readonly repository?: string;
    readonly bugs?: string;
    readonly markdown?: string;
    readonly galleryColor?: string;
    readonly galleryTheme?: string;
    readonly qna?: string;
    readonly badges?: Badge[];
    readonly dependencies?: ExtensionReference[];
    readonly bundledExtensions?: ExtensionReference[];
}

export interface Badge {
    url: string;
    href: string;
    description: string;
}

export interface ExtensionReference {
    publisher: string;
    extension: string;
    version?: string;
}

export type StarRating = 1 | 2 | 3 | 4 | 5;
export interface NewReview {
    rating: StarRating;
    title: string;
    comment: string;
}

export interface ExtensionReview extends NewReview {
    user: UserData;
    timestamp: string;
}

export interface ExtensionReviewList {
    postUrl: string;
    reviews: ExtensionReview[];
}

export interface UserData {
    loginName: string;
    tokensUrl: string;
    createTokenUrl: string;
    fullName?: string;
    avatarUrl?: string;
}

export interface PersonalAccessToken {
    id: number;
    value?: string;
    createdTimestamp: string;
    accessedTimestamp: string;
    description: string;
    deleteTokenUrl: string;
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
