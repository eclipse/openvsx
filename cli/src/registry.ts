/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as http from 'http';
import * as https from 'https';
import * as fs from 'fs';
import * as querystring from 'querystring';
import * as followRedirects from 'follow-redirects';
import { statusError } from './util';

export const DEFAULT_URL = 'https://open-vsx.org';
export const DEFAULT_NAMESPACE_SIZE = 1024;
export const DEFAULT_PUBLISH_SIZE = 512 * 1024 * 1024;

export class Registry {

    readonly url: string;
    readonly maxNamespaceSize: number;
    readonly maxPublishSize: number;

    constructor(options: RegistryOptions = {}) {
        if (options.url && options.url.endsWith('/'))
            this.url = options.url.substring(0, options.url.length - 1);
        else if (options.url)
            this.url = options.url;
        else
            this.url = DEFAULT_URL;
        this.maxNamespaceSize = options.maxNamespaceSize || DEFAULT_NAMESPACE_SIZE;
        this.maxPublishSize = options.maxPublishSize || DEFAULT_PUBLISH_SIZE;
    }

    get requiresLicense(): boolean {
        const url = new URL(this.url);
        return url.hostname === 'open-vsx.org' || url.hostname.endsWith('.open-vsx.org');
    }

    createNamespace(name: string, pat: string): Promise<Response> {
        try {
            const query: { [key: string]: string } = { token: pat };
            const url = this.getUrl('api/-/namespace/create', query);
            const namespace = { name };
            return this.post(JSON.stringify(namespace), url, {
                'Content-Type': 'application/json'
            }, this.maxNamespaceSize);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    publish(file: string, pat: string): Promise<Extension> {
        try {
            const query: { [key: string]: string } = { token: pat };
            const url = this.getUrl('api/-/publish', query);
            return this.postFile(file, url, {
                'Content-Type': 'application/octet-stream'
            }, this.maxPublishSize);
        } catch (err) {
            return Promise.reject(err);
        }
    }

    getMetadata(namespace: string, extension: string): Promise<Extension> {
        try {
            const path = `api/${encodeURIComponent(namespace)}/${encodeURIComponent(extension)}`;
            return this.getJson(this.getUrl(path));
        } catch (err) {
            return Promise.reject(err);
        }
    }

    download(file: string, url: URL): Promise<void> {
        return new Promise((resolve, reject) => {
            const stream = fs.createWriteStream(file);
            const request = this.getProtocol(url)
                                .request(url, response => {
                response.on('end', () => {
                    if (response.statusCode !== undefined && (response.statusCode < 200 || response.statusCode > 299)) {
                        reject(statusError(response));
                    } else {
                        resolve();
                    }
                });
                response.pipe(stream);
            });
            stream.on('error', err => {
                request.abort();
                reject(err);
            });
            request.on('error', err => {
                stream.close();
                reject(err);
            });
            request.end();
        });
    }

    getJson<T extends Response>(url: URL): Promise<T> {
        return new Promise((resolve, reject) => {
            const request = this.getProtocol(url)
                                .request(url, this.getJsonResponse<T>(resolve, reject));
            request.on('error', reject);
            request.end();
        });
    }

    post<T extends Response>(content: string | Buffer | Uint8Array, url: URL, headers?: http.OutgoingHttpHeaders, maxBodyLength?: number): Promise<T> {
        return new Promise((resolve, reject) => {
            const requestOptions = { method: 'POST', headers, maxBodyLength } as http.RequestOptions;
            const request = this.getProtocol(url)
                                .request(url, requestOptions, this.getJsonResponse<T>(resolve, reject));
            request.on('error', reject);
            request.write(content);
            request.end();
        });
    }

    postFile<T extends Response>(file: string, url: URL, headers?: http.OutgoingHttpHeaders, maxBodyLength?: number): Promise<T> {
        return new Promise((resolve, reject) => {
            const stream = fs.createReadStream(file);
            const requestOptions = { method: 'POST', headers, maxBodyLength } as http.RequestOptions;
            const request = this.getProtocol(url)
                                .request(url, requestOptions, this.getJsonResponse<T>(resolve, reject));
            stream.on('error', err => {
                request.abort();
                reject(err);
            });
            request.on('error', err => {
                stream.close();
                reject(err);
            });
            stream.on('open', () => stream.pipe(request));
        });
    }

    private getUrl(path: string, query?: { [key: string]: string }): URL {
        const url = new URL(this.url);
        url.pathname += path;
        if (query) {
            url.search = querystring.stringify(query);
        }
        return url;
    }

    private getProtocol(url: URL) {
        if (url.protocol === 'https:')
            return followRedirects.https as typeof https;
        else
            return followRedirects.http as typeof http;
    }

    private getJsonResponse<T extends Response>(resolve: (value: T) => void, reject: (reason: any) => void): (res: http.IncomingMessage) => void {
        return response => {
            response.setEncoding('UTF-8');
            let json = '';
            response.on('data', chunk => json += chunk);
            response.on('end', () => {
                if (response.statusCode !== undefined && (response.statusCode < 200 || response.statusCode > 299)) {
                    if (json.startsWith('{')) {
                        try {
                            const parsed = JSON.parse(json) as ErrorResponse;
                            const message = parsed.message || parsed.error;
                            if (message) {
                                reject(new Error(message));
                                return;
                            }
                        } catch (err) {
                            // Ignore the error and reject with response status
                        }
                    }
                    reject(statusError(response));
                } else if (json.startsWith('<!DOCTYPE html>')) {
                    reject(json);
                } else {
                    try {
                        resolve(JSON.parse(json));
                    } catch (err) {
                        reject(err);
                    }
                }
            });
        };
    }

}

export interface RegistryOptions {
    url?: string;
    maxNamespaceSize?: number;
    maxPublishSize?: number;
}

export interface Response {
    success?: string;
    error?: string;
}

export interface Extension extends Response {
    namespaceUrl: string;
    reviewsUrl: string;
    // key: file type, value: url
    files: { [type: string]: string };

    name: string;
    namespace: string;
    version: string;
    publishedBy: UserData;
    verified: boolean;
    // key: version, value: url
    allVersions: { [version: string]: string };

    averageRating?: number;
    downloadCount: number;
    reviewCount: number;

    versionAlias: string[];
    timestamp: string;
    preview?: boolean;
    displayName?: string;
    description?: string;

    // key: engine, value: version constraint
    engines?: { [engine: string]: string };
    categories?: string[];
    tags?: string[];
    license?: string;
    homepage?: string;
    repository?: string;
    bugs?: string;
    markdown?: string;
    galleryColor?: string;
    galleryTheme?: string;
    qna?: string;
    badges?: Badge[];
    dependencies?: ExtensionReference[];
    bundledExtensions?: ExtensionReference[];
}

export interface UserData {
    loginName: string;
    fullName?: string;
    avatarUrl?: string;
    homepage?: string;
}

export interface Badge {
    url: string;
    href: string;
    description: string;
}

export interface ExtensionReference {
    url: string;
    namespace: string;
    extension: string;
    version?: string;
}

export interface ErrorResponse {
    error: string;
    message: string;
    status: number;
    path?: string;
    timestamp?: string;
    trace?: string;
}
