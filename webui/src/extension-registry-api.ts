/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import {
    Extension, UserData, ExtensionReview, ExtensionReviewList, SearchResult
} from "./extension-registry-types";

export interface ServerAPIRequest<Res> {
    endpoint: string;
    operation: (response: Response) => Promise<Res>;
    credentials?: boolean;
}

export interface ServerAPIRequestWithoutPayload<Res> extends ServerAPIRequest<Res> {
    method: 'GET' | 'DELETE';
}

export interface ServerAPIRequestWithPayload<Res, Req> extends ServerAPIRequest<Res> {
    method: 'POST' | 'PUT';
    payload: Req;
    contentType: string;
}

export interface ErrorResponse {
    error: string;
    message: string;
    path: string;
    status: number;
    timestamp: string;
    trace: string;
}

export class ExtensionRegistryAPI {

    protected async run<Res>(req: ServerAPIRequestWithPayload<Res, any> | ServerAPIRequestWithoutPayload<Res>): Promise<Res> {
        const param: RequestInit = {
            method: req.method
        };
        const headers: Record<string, string> = {
            'Content-Type': 'application/json'
        };
        if (req.method === 'POST' || req.method === 'PUT') {
            param.body = JSON.stringify(req.payload);
            headers['Accept'] = req.contentType;
        }
        if (req.credentials) {
            param.credentials = 'include';
        }
        param.headers = headers;

        const response = await fetch(req.endpoint, param);
        if (response.status === 200) {
            return req.operation(response);
        } else {
            throw await response.json() as ErrorResponse;
        }
    }

    getExtensions(endpoint: string): Promise<SearchResult> {
        return this.run<SearchResult>({
            method: 'GET',
            endpoint,
            operation: response => response.json()
        });
    }

    getExtension(endpoint: string): Promise<Extension> {
        return this.run<Extension>({
            method: 'GET',
            endpoint,
            operation: response => response.json()
        });
    }

    getExtensionReadme(endpoint: string): Promise<string> {
        return this.run<string>({
            method: 'GET',
            endpoint,
            operation: response => response.text()
        });
    }

    getExtensionReviews(endpoint: string): Promise<ExtensionReviewList> {
        return this.run<ExtensionReviewList>({
            method: 'GET',
            endpoint,
            operation: response => response.json()
        });
    }

    postReview(payload: ExtensionReview, endpoint: string): Promise<void> {
        return this.run({
            method: 'POST',
            payload,
            contentType: 'application/json;charset=UTF-8',
            credentials: true,
            endpoint,
            operation: response => response.json()
        });
    }

    getUser(endpoint: string): Promise<UserData | ErrorResponse> {
        return this.run({
            method: 'GET',
            credentials: true,
            endpoint,
            operation: response => response.json()
        });
    }
}