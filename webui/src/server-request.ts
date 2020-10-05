/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

export interface ServerAPIRequest {
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
        method: req.method
    };
    if (req.payload) {
        param.body = JSON.stringify(req.payload);
    }
    param.headers = req.headers;
    if (req.followRedirect) {
        param.redirect = 'follow';
    }
    if (req.credentials) {
        param.credentials = 'include';
    }

    const response = await fetch(req.endpoint, param);
    if (response.ok) {
        switch (req.headers!['Accept']) {
            case 'application/json':
                return response.json();
            case 'text/plain':
                return response.text() as Promise<any>;
            default:
                throw new Error(`Unsupported type ${req.headers!['Accept']}`);
        }
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
