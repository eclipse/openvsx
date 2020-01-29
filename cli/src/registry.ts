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
import { statusError } from './util';

export const DEFAULT_URL = 'http://localhost:8080';

export class Registry {

    readonly url: string;

    constructor(options: RegistryOptions = {}) {
        if (options.url && options.url.endsWith('/'))
            this.url = options.url.substring(0, options.url.length - 1);
        else if (options.url)
            this.url = options.url;
        else
            this.url = DEFAULT_URL;
    }

    publish(file: string, pat?: string): Promise<Extension> {
        try {
            const url = this.getUrl('api/-/publish', pat ? { token: pat } : undefined);
            return this.post(file, url, {
                'Content-Type': 'application/octet-stream'
            });
        } catch (err) {
            return Promise.reject(err);
        }
    }

    getMetadata(publisher: string, extension: string): Promise<Extension> {
        try {
            const path = `api/${encodeURIComponent(publisher)}/${encodeURIComponent(extension)}`;
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

    post<T extends Response>(file: string, url: URL, headers?: http.OutgoingHttpHeaders): Promise<T> {
        return new Promise((resolve, reject) => {
            const stream = fs.createReadStream(file);
            const request = this.getProtocol(url)
                                .request(url, { method: 'POST', headers }, this.getJsonResponse<T>(resolve, reject));
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
            return https;
        else
            return http;
    }

    private getJsonResponse<T extends Response>(resolve: (value: T) => void, reject: (reason: any) => void): (res: http.IncomingMessage) => void {
        return response => {
            response.setEncoding('UTF-8');
            let json = '';
            response.on('data', chunk => json += chunk);
            response.on('end', () => {
                if (response.statusCode !== undefined && (response.statusCode < 200 || response.statusCode > 299)) {
                    reject(statusError(response));
                } else if (json.startsWith('<!DOCTYPE html>')) {
                    reject(json);
                } else {
                    resolve(JSON.parse(json));
                }
            });
        };
    }

}

export interface RegistryOptions {
    url?: string;
}

export interface Response {
    error?: string;
}

export interface Extension extends Response {
    name: string;
    publisher: string;
    displayName?: string;
    version: string;
    preview?: boolean;
    timestamp?: string;
    description?: string;
    averageRating?: number;
    reviewCount?: number;

    url: string;
    iconUrl?: string;
    publisherUrl: string;
    reviewsUrl: string;
    downloadUrl: string;
    readmeUrl?: string;

    allVersions: { [key: string]: string };
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
