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

export function createAbsoluteURL(arr: string[], queries?: { key: string, value: string | number }[]): string {
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
            if (err.message === 'Failed to fetch') {
                return 'The registry server is not available. Please contact the site administrators.';
            }
            return `An unexpected error occurred: ${err.message}`;
        } else if (err.error && err.status && err.message) {
            return `The server responded with an error: ${err.error} (status ${err.status}, ${err.message})`;
        } else if (err.error && err.status) {
            return `The server responded with an error: ${err.error} (status ${err.status})`;
        } else if (err.error) {
            return `The server responded with an error: ${err.error}`;
        }
    }
    return 'An unexpected error occurred.';
}
