/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

export interface ServerAPIRequest<Res> {
    endpoint: string;
    method?: 'GET' | 'DELETE' | 'POST' | 'PUT';
    accept?: 'application/json' | 'text/plain';
    credentials?: boolean;
}

export interface ServerAPIRequestWithPayload<Res, Req> extends ServerAPIRequest<Res> {
    method: 'POST' | 'PUT';
    payload: Req;
    contentType: string;
}

export function hasPayload<Res>(req: ServerAPIRequest<Res>): req is ServerAPIRequestWithPayload<Res, any> {
    return req.method === 'POST' || req.method === 'PUT';
}

export interface ErrorResponse {
    error: string;
    message: string;
    status: number;
    path?: string;
    timestamp?: string;
    trace?: string;
}

export async function sendRequest<Res>(req: ServerAPIRequest<Res> | ServerAPIRequestWithPayload<Res, any>): Promise<Res> {
    if (!req.method) {
        req.method = 'GET';
    }
    if (!req.accept) {
        req.accept = 'application/json';
    }

    const param: RequestInit = {
        method: req.method
    };
    const headers: Record<string, string> = {
        'Accept': req.accept
    };
    if (hasPayload(req)) {
        param.body = JSON.stringify(req.payload);
        headers['Content-Type'] = req.contentType;
    }
    param.headers = headers;
    if (req.credentials) {
        param.credentials = 'include';
    }

    const response = await fetch(req.endpoint, param);
    if (response.ok) {
        switch (req.accept) {
            case 'application/json':
                return response.json();
            case 'text/plain':
                return response.text() as Promise<any>;
            default:
                throw new Error(`Unsupported type ${req.accept}`);
        }
    } else {
        let err: ErrorResponse;
        try {
            err = await response.json();
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
