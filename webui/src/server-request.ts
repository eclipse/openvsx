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
    operation?: (response: Response) => Promise<Res>;
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
    path: string;
    status: number;
    timestamp: string;
    trace: string;
}

export async function sendRequest<Res>(req: ServerAPIRequest<Res> | ServerAPIRequestWithPayload<Res, any>): Promise<Res> {
    const param: RequestInit = {
        method: req.method || 'GET'
    };
    const headers: Record<string, string> = {
        'Content-Type': 'application/json'
    };
    if (hasPayload(req)) {
        param.body = JSON.stringify(req.payload);
        headers['Accept'] = req.contentType;
    }
    if (req.credentials) {
        param.credentials = 'include';
    }
    param.headers = headers;

    const response = await fetch(req.endpoint, param);
    if (response.status === 200) {
        if (req.operation) {
            return req.operation(response);
        } else {
            return response.json();
        }
    } else {
        throw await response.json() as ErrorResponse;
    }
}
