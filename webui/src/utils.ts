/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { ErrorResponse } from './server-request';

export function addQuery(url: string, queries: { key: string, value: string | number | undefined }[]): string {
    const nonEmpty = queries.filter(q => q.value !== undefined);
    if (nonEmpty.length === 0) {
        return url;
    }
    return url + '?' + nonEmpty.map<string>(q => q.key + '=' + encodeURIComponent(q.value!)).join('&');
}

export function createAbsoluteURL(arr: string[], queries?: { key: string, value: string | number | undefined }[]): string {
    const url = arr.length === 0 ? '' : arr.reduce((prev, curr) => prev + '/' + curr);
    if (queries && queries.length > 0) {
        return addQuery(url, queries);
    } else {
        return url;
    }
}

export function createRoute(arr: string[], queries?: { key: string, value: string | number }[]): string {
    const url = createAbsoluteURL(arr, queries);
    return url.startsWith('/') ? url : '/' + url;
}

export function debounce(task: () => void, token: { timeout?: number }, delay: number = 150): void {
    clearTimeout(token.timeout);
    token.timeout = window.setTimeout(task, delay);
}

export function toLocalTime(timestamp?: string): string | undefined {
    if (!timestamp) {
        return undefined;
    }
    const date = new Date(timestamp);
    const options: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
    };
    return new Intl.DateTimeFormat(undefined, options).format(date);
}

const msPerMinute = 60 * 1000;
const msPerHour = msPerMinute * 60;
const msPerDay = msPerHour * 24;
const msPerMonth = msPerDay * 30.4;
const msPerYear = msPerDay * 365;
export function toRelativeTime(timestamp?: string): string | undefined {
    if (!timestamp) {
        return undefined;
    }
    const date = new Date(timestamp);
    const elapsed = Date.now() - date.getTime();
    if (elapsed < msPerMinute) {
        return 'now';
    } else if (elapsed < msPerHour) {
        const value = Math.round(elapsed / msPerMinute);
        return `${value} minute${value !== 1 ? 's' : ''} ago`;
    } else if (elapsed < msPerDay) {
        const value = Math.round(elapsed / msPerHour);
        return `${value} hour${value !== 1 ? 's' : ''} ago`;
    } else if (elapsed < msPerMonth) {
        const value = Math.round(elapsed / msPerDay);
        return `${value} day${value !== 1 ? 's' : ''} ago`;
    } else if (elapsed < msPerYear) {
        const value = Math.round(elapsed / msPerMonth);
        return `${value} month${value !== 1 ? 's' : ''} ago`;
    } else {
        const value = Math.round(elapsed / msPerYear);
        return `${value} year${value !== 1 ? 's' : ''} ago`;
    }
}

export function handleError(err?: Error | Partial<ErrorResponse>): string {
    if (err) {
        console.error(err);
        if (err instanceof Error) {
            if (err.message === 'Failed to fetch'
                || err.message === 'Unexpected token < in JSON at position 0') {
                return 'Something went wrong while fetching data. Please contact the site administrators.';
            }
            return `An unexpected error occurred: ${err.message}`;
        } else if (err.error && err.message) {
            return `${err.error} (${err.message})`;
        } else if (err.error) {
            return err.error;
        } else if (err.message) {
            return err.message;
        }
    }
    return 'An unexpected error occurred.';
}

export interface Cookie {
    key: string;
    value: string;
    path?: string;
    domain?: string;
    maxAge?: number;
    expires?: string;
    secure?: boolean;
    samesite?: 'lax' | 'strict' | 'none';
}

export function setCookie(cookie: Cookie): void {
    let cookieString = `${cookie.key}=${encodeURIComponent(cookie.value)}`;
    if (cookie.path) {
        cookieString += `; path=${cookie.path}`;
    }
    if (cookie.domain) {
        cookieString += `; domain=${cookie.domain}`;
    }
    if (cookie.maxAge) {
        cookieString += `; max-age=${cookie.maxAge}`;
    }
    if (cookie.expires) {
        cookieString += `; expires=${cookie.expires}`;
    }
    if (cookie.secure) {
        cookieString += '; secure';
    }
    if (cookie.samesite) {
        cookieString += `; samesite=${cookie.samesite}`;
    }
    document.cookie = cookieString;
}

const cookieRegexp = /(?<key>[^ \t=]+)\s*=\s*(?<value>[^;]+)(?:;|$)/g;

export function getCookieValueByKey(key: string): string | undefined {
    const matches = document.cookie.matchAll(cookieRegexp);
    for (const match of matches) {
        if (match[1] === key) {
            return decodeURIComponent(match[2]);
        }
    }
    return undefined;
}

namespace TargetPlatform {
    export const WIN32_IA32 = 'win32-ia32';
    export const WIN32_X64 = 'win32-x64';
    export const WIN32_ARM64 = 'win32-arm64';
    export const LINUX_X64 = 'linux-x64';
    export const LINUX_ARM64 = 'linux-arm64';
    export const LINUX_ARMHF = 'linux-armhf';
    export const ALPINE_X64 = 'alpine-x64';
    export const ALPINE_ARM64 = 'alpine-arm64';
    export const DARWIN_X64 = 'darwin-x64';
    export const DARWIN_ARM64 = 'darwin-arm64';
    export const WEB = 'web';
    export const UNIVERSAL = 'universal';
}

export function getTargetPlatforms(): string[] {
    return [
        TargetPlatform.WIN32_IA32,
        TargetPlatform.WIN32_X64,
        TargetPlatform.WIN32_ARM64,
        TargetPlatform.LINUX_X64,
        TargetPlatform.LINUX_ARM64,
        TargetPlatform.LINUX_ARMHF,
        TargetPlatform.ALPINE_X64,
        TargetPlatform.ALPINE_ARM64,
        TargetPlatform.DARWIN_X64,
        TargetPlatform.DARWIN_ARM64,
        TargetPlatform.WEB,
        TargetPlatform.UNIVERSAL
    ];
}

export function getTargetPlatformDisplayName(targetPlatform: string): string {
    const targetPlatformDisplayNames = new Map([
        [TargetPlatform.UNIVERSAL, 'Universal'],
        [TargetPlatform.WIN32_IA32, 'Windows x86'],
        [TargetPlatform.WIN32_X64, 'Windows x64'],
        [TargetPlatform.WIN32_ARM64, 'Windows ARM'],
        [TargetPlatform.LINUX_X64, 'Linux x64'],
        [TargetPlatform.LINUX_ARM64, 'Linux ARM64'],
        [TargetPlatform.LINUX_ARMHF, 'Linux ARMhf'],
        [TargetPlatform.ALPINE_X64, 'Alpine Linux 64 bit'],
        [TargetPlatform.ALPINE_ARM64, 'Alpine Linux ARM64'],
        [TargetPlatform.DARWIN_X64, 'macOS Intel'],
        [TargetPlatform.DARWIN_ARM64, 'macOS Apple Silicon'],
        [TargetPlatform.WEB, 'Web']
    ]);

    return targetPlatformDisplayNames.get(targetPlatform) || '';
}
