/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { utcToZonedTime } from "date-fns-tz";
import { ErrorResponse } from "./server-request";

export function addQuery(url: string, queries: { key: string, value: string | number }[]) {
    return url + '?' + queries.map<string>((obj) => obj.key + '=' + obj.value).join('&');
}

export function createAbsoluteURL(arr: string[], queries?: { key: string, value: string | number }[]): string {
    const url = arr.reduce((prev, curr) => prev !== undefined ? prev + '/' + curr : curr);
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

export function debounce(task: () => void, token: { timeout?: number }, delay: number = 150) {
    clearTimeout(token.timeout);
    token.timeout = setTimeout(task, delay);
}

export function toLocalTime(timestamp?: string): Date | undefined {
    if (!timestamp) {
        return undefined;
    }
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return utcToZonedTime(timestamp, timeZone);
}

export function handleError(err?: Error | Partial<ErrorResponse>): void {
    if (err) {
        console.error(err);
        if (err instanceof Error)
            alert(`An unexpected error occurred: ${err.message}`);
        else if (err.error && err.status && err.message)
            alert(`The server responded with an error: ${err.error} (status ${err.status}, ${err.message})`);
        else if (err.error && err.status)
            alert(`The server responded with an error: ${err.error} (status ${err.status})`);
        else if (err.error)
            alert(`The server responded with an error: ${err.error}`);
    }
}
