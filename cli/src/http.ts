/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import * as http from 'http';
import * as https from 'https';
import * as fs from 'fs';
import { redactUrl, statusError, withStatus } from './util';

// Hop limit the fetch standard uses.
const MAX_REDIRECTS = 20;

// Dropped on a redirect to another origin; X-OpenVSX-Token carries the PAT just like Authorization.
const CREDENTIAL_HEADERS = /^(?:authorization|cookie|proxy-authorization|x-openvsx-token)$/i;

export type RequestBody = () => string | Buffer | Uint8Array | fs.ReadStream;

/**
 * Sends a request and follows redirects to `onResponse`'s final response, reporting any failure
 * to `fail`. A GET follows every redirect; a request with a body follows only 307/308, sending
 * the body again, because 301/302/303 turn it into a GET without one. Credentials are dropped
 * once a redirect leaves the origin. Only the headers in `options` are sent, so a caller that
 * passes none carries no credentials.
 */
export function send(
    url: URL,
    options: http.RequestOptions,
    body: RequestBody | undefined,
    onResponse: (response: http.IncomingMessage) => void,
    fail: (err: Error) => void,
    hops = 0
): void {
    const protocol = url.protocol === 'https:' ? https : http;
    const request = protocol.request(url, options, response => {
        const status = response.statusCode ?? 0;
        const location = response.headers.location;
        if (status < 300 || status > 399 || !location) {
            onResponse(response);
            return;
        }
        response.resume();

        let next: URL;
        try {
            next = new URL(location, url);
        } catch (err) {
            fail(err as Error);
            return;
        }
        if (hops >= MAX_REDIRECTS) {
            fail(new Error(`The request to ${redactUrl(url)} was redirected more than ${MAX_REDIRECTS} times.`));
        } else if (next.protocol !== 'https:' && next.protocol !== 'http:') {
            fail(new Error(`The request to ${redactUrl(url)} was redirected to unsupported URL ${redactUrl(next)}.`));
        } else if (body && status !== 307 && status !== 308) {
            fail(withStatus(new Error(`The ${options.method} request to ${redactUrl(url)} was redirected to ${redactUrl(next)} `
                + `with status ${status}, which drops the request body. Use the redirect target as the registry URL.`), status));
        } else {
            // The server may answer before reading the body: abandon this hop so its stream
            // releases the file and a late reset on its socket can't fail the next hop.
            request.removeListener('error', fail);
            request.on('error', () => { /* abandoned */ });
            if (content instanceof fs.ReadStream) {
                content.unpipe(request);
                content.destroy();
            }
            request.destroy();
            const headers = next.origin === url.origin ? options.headers : withoutCredentials(options.headers as http.OutgoingHttpHeaders);
            send(next, { ...options, headers }, body, onResponse, fail, hops + 1);
        }
    });
    request.on('error', fail);
    failOnTimeout(request, url, options.timeout, fail);

    const content = body?.();
    if (content instanceof fs.ReadStream) {
        content.on('error', (err: Error) => {
            request.destroy();
            fail(err);
        });
        request.on('error', () => content.close());
        content.on('open', () => content.pipe(request));
    } else {
        request.end(content);
    }
}

/** Sends a GET with only the given headers and resolves with the parsed JSON response. */
export function getJson<T>(url: URL, headers: http.OutgoingHttpHeaders, timeout: number): Promise<T> {
    return new Promise((resolve, reject) => {
        send(url, { method: 'GET', headers, timeout }, undefined, jsonResponse<T>(resolve, reject), reject);
    });
}

/**
 * Reads a response as JSON. A non-2xx status rejects with the `message` or `error` of a JSON
 * error body when there is one, and with the status otherwise. An HTML page rejects with an
 * Error rather than its markup, since it usually means the URL points at a website, not an API.
 */
export function jsonResponse<T>(resolve: (value: T) => void, reject: (reason: any) => void): (res: http.IncomingMessage) => void {
    return response => {
        response.setEncoding('utf-8');
        let json = '';
        // A connection lost after the headers have arrived ends the response without 'end' ever
        // firing, so without this the promise is never settled and the command waits on a body
        // that is not coming. The configured timeout would eventually rescue it, but rejecting
        // here reports what actually happened instead of thirty seconds of silence.
        response.on('error', reject);
        response.on('data', chunk => json += chunk);
        response.on('end', () => {
            if (response.statusCode !== undefined && (response.statusCode < 200 || response.statusCode > 299)) {
                if (json.trimStart().startsWith('{')) {
                    try {
                        const parsed = JSON.parse(json) as { message?: string; error?: string };
                        const message = parsed.message || parsed.error;
                        if (message) {
                            // keep the status: the message alone cannot say whether retrying is worth it
                            reject(withStatus(new Error(message), response.statusCode));
                            return;
                        }
                    } catch (err) {
                        // Ignore the error and reject with response status
                    }
                }
                reject(statusError(response));
            } else if (isHtml(response, json)) {
                reject(withStatus(new Error(`The server responded with status ${response.statusCode} and an HTML page instead of JSON.`),
                    response.statusCode));
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

/**
 * Node's `timeout` option only raises an event - the request stays open, which is why a server
 * that accepts a connection and then says nothing used to hold a command open indefinitely.
 * Destroying the request with an error routes through the error handling each caller already
 * has, so a stalled request rejects the way any other failure does. With a timeout of zero the
 * event never fires and this does nothing.
 */
function failOnTimeout(request: http.ClientRequest, url: URL, timeout: number | undefined, fail: (err: Error) => void): void {
    request.on('timeout', () => {
        // Reported before the request is torn down, rather than by destroying it with the error.
        // Destroying raises errors of its own - ECONNRESET on the response, a premature close
        // from pipeline - and node guarantees no order between those and the request's own. In
        // practice the request's arrives first, so this is belt and braces rather than the thing
        // that stops the wrong error being reported; that is the caller claiming the failure
        // before it does any asynchronous cleanup.
        fail(new Error(`No response from ${redactUrl(url)} for ${timeout} ms.`));
        request.destroy();
    });
}

// The body check covers servers that send HTML without a text/html Content-Type.
function isHtml(response: http.IncomingMessage, body: string): boolean {
    const contentType = response.headers['content-type']?.split(';')[0].trim().toLowerCase();
    return contentType === 'text/html' || /^\s*<(?:!doctype\s+html|html)[\s>]/i.test(body);
}

function withoutCredentials(headers: http.OutgoingHttpHeaders = {}): http.OutgoingHttpHeaders {
    return Object.fromEntries(Object.entries(headers).filter(([name]) => !CREDENTIAL_HEADERS.test(name)));
}
