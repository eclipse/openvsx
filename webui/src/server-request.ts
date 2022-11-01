/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import fetchBuilder from "fetch-retry";

export interface ServerAPIRequest {
    abortController: AbortController;
    endpoint: string;
    method?: 'GET' | 'DELETE' | 'POST' | 'PUT';
    headers?: Record<string, string>;
    followRedirect?: boolean;
    credentials?: boolean;
    payload?: any;
}

export interface ErrorResponse {
    error: string;
    message: string;
    status: number;
    path?: string;
    timestamp?: string;
    trace?: string;
}

export async function sendRequest<Res>(req: ServerAPIRequest): Promise<Res> {
    if (!req.method) {
        req.method = 'GET';
    }
    if (!req.headers) {
        req.headers = {};
    }
    if (!req.headers['Accept']) {
        req.headers['Accept'] = 'application/json';
    }

    const param: RequestInit = {
        method: req.method,
        signal: req.abortController.signal
    };
    if (req.payload) {
        param.body = (req.payload instanceof File) ? req.payload : JSON.stringify(req.payload);
    }
    param.headers = req.headers;
    if (req.followRedirect) {
        param.redirect = 'follow';
    }
    if (req.credentials) {
        param.credentials = 'include';
    }

    const options: any = {
        retries: 10,
        retryDelay: (attempt: number, error: Error, response: Response) => {
            return Math.pow(2, attempt) * 1000;
        },
        retryOn: (attempt: number, error: Error, response: Response) => {
            return error !== null || response.status >= 500;
        }
    };

    const response = await fetchBuilder(fetch, options)(req.endpoint, param);
    if (response.ok) {
        switch (req.headers!['Accept']) {
            case 'application/json':
                return response.json();
            case 'text/plain':
                return response.text() as Promise<any>;
            case 'application/octet-stream':
                return response.blob() as Promise<any>;
            default:
                throw new Error(`Unsupported type ${req.headers!['Accept']}`);
        }
    } else if (response.status === 429) {
        const retrySeconds = response.headers.get('X-Rate-Limit-Retry-After-Seconds') || '0';
        const jitter = Math.floor(Math.random() * 100);
        const timeoutMillis = ((Number(retrySeconds) + 1) * 1000) + jitter;
        return new Promise<ServerAPIRequest>(resolve => setTimeout(resolve, timeoutMillis, req))
            .then(request => sendRequest(request));
    } else {
        let err: ErrorResponse;
        try {
            err = await response.json();
            err.status = response.status;
        } catch (_) {
            err = {
                error: `Request failed: ${req.method} ${response.url}`,
                status: response.status,
                message: response.statusText || `Status ${response.status}`
            };
        }
        throw err;
    }
}
