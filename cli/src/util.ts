/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as fs from 'fs';
import * as path from 'path';
import * as tmp from 'tmp';
import * as http from 'http';
import { RegistryOptions } from './registry-options';
import { TrustedPublishingOptions } from './trusted-publishing-options';

export { promisify } from 'util';

export function addEnvOptions(options: RegistryOptions): void {
    options.registryUrl ??= process.env.OVSX_REGISTRY_URL;
    options.pat ??= process.env.OVSX_PAT;
    options.username ??= process.env.OVSX_USERNAME;
    options.password ??= process.env.OVSX_PASSWORD;
}

export function addTrustedPublishingEnvOptions(options: TrustedPublishingOptions): void {
    options.trustedPublishing ??= parseBooleanEnv(process.env.OVSX_TRUSTED_PUBLISHING);
    options.idToken ??= process.env.OVSX_ID_TOKEN;
    options.oidcAudience ??= process.env.OVSX_OIDC_AUDIENCE;
}

function parseBooleanEnv(value?: string): boolean | undefined {
    if (value === undefined || value.trim().length === 0) {
        return undefined;
    }
    return ['true', '1', 'yes'].includes(value.trim().toLowerCase());
}

/**
 * Parses a commander option that must be a non-negative whole number, so a typo is reported by the
 * CLI rather than sent to the registry as a bad request.
 */
export function parsePositiveInt(value: string): number {
    const parsed = Number(value);
    if (!Number.isInteger(parsed) || parsed < 0) {
        throw new Error(`Expected a non-negative whole number, got '${value}'.`);
    }
    return parsed;
}

export function matchExtensionId(id: string): RegExpExecArray | null {
    return /^([\w-]+)(?:\.|\/)([\w-]+)$/.exec(id);
}

export function optionalStat(path: fs.PathLike): Promise<fs.Stats | undefined> {
    return new Promise((resolve, reject) => {
        fs.stat(path, (err, stats) => resolve(stats));
    });
}

export function makeDirs(path: fs.PathLike): Promise<void> {
    return new Promise((resolve, reject) => {
        if (fs.existsSync(path)) {
            resolve();
        } else {
            fs.mkdir(path, { recursive: true }, (err: NodeJS.ErrnoException | null) => {
                if (err)
                    reject(err);
                else
                    resolve();
            });
        }
    });
}

export function createTempFile(options: tmp.TmpNameOptions): Promise<string> {
    return new Promise((resolve, reject) => {
        tmp.tmpName(options, (err: Error | null, name: string) => {
            if (err)
                reject(err);
            else
                resolve(name);
        });
    });
}

export function rejectError(err: any) {
    const reason = err instanceof Error ? err : new Error(String(err));
    return Promise.reject(reason);
}

export function handleError(debug?: boolean, additionalMessage?: string, exit: boolean = true): (reason: any) => void {
    return reason => {
        if (reason instanceof Error && !debug) {
            console.error(`\u274c  ${reason.message}`);
            if (additionalMessage) {
                console.error(additionalMessage);
            }
        } else if (typeof reason === 'string') {
            console.error(`\u274c  ${reason}`);
        } else if (reason !== undefined) {
            console.error(reason);
        } else {
            console.error('An unknown error occurred.');
        }

        if (exit) {
            process.exit(1);
        }
    };
}

/**
 * Formats a byte count for display, e.g. `1536` -> `1.5 KB`. Mirrors the registry's own
 * `FileUtils.byteCountToDisplaySize` formatting so client- and server-side size limit messages read
 * the same way.
 */
export function formatBytes(bytes: number): string {
    const units = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB'];
    let size = bytes;
    let unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
        size /= 1024;
        unit++;
    }
    return `${Math.floor(size)} ${units[unit]}`;
}

/**
 * An error carrying the HTTP status that produced it, so a caller can tell "the registry refused this"
 * from "the registry could not answer right now" without matching on the message.
 */
export interface StatusError extends Error {
    status?: number;
}

export function withStatus(error: Error, status?: number): StatusError {
    const withStatusCode = error as StatusError;
    withStatusCode.status = status;
    return withStatusCode;
}

export function statusError(response: http.IncomingMessage): StatusError {
    const message = response.statusMessage
        ? `The server responded with status ${response.statusCode}: ${response.statusMessage}`
        : `The server responded with status ${response.statusCode}.`;
    return withStatus(new Error(message), response.statusCode);
}

export function readFile(name: string, packagePath?: string, encoding: BufferEncoding = 'utf-8'): Promise<string> {
    return new Promise((resolve, reject) => {
        fs.readFile(
            path.join(packagePath ?? process.cwd(), name),
            { encoding },
            (err: NodeJS.ErrnoException | null, content: string) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(content);
                }
            }
        );
    });
}

export async function readManifest(packagePath?: string): Promise<Manifest> {
    const content = await readFile('package.json', packagePath);
    return JSON.parse(content);
}

export function validateManifest(manifest: Manifest): void {
    if (!manifest.publisher) {
        throw new Error("Missing required field 'publisher'.");
    }
    if (!manifest.name) {
        throw new Error("Missing required field 'name'.");
    }
    if (!manifest.version) {
        throw new Error("Missing required field 'version'.");
    }
}

export function writeFile(name: string, content: string, packagePath?: string, encoding: BufferEncoding = 'utf-8'): Promise<void> {
    return new Promise((resolve, reject) => {
        fs.writeFile(
            path.join(packagePath ?? process.cwd(), name),
            content,
            { encoding },
            (err: NodeJS.ErrnoException | null) => {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            }
        );
    });
}

export function writeManifest(manifest: Manifest, packagePath?: string): Promise<void> {
    const content = JSON.stringify(manifest, null, 4);
    return writeFile('package.json', content, packagePath);
}

export interface Manifest {
    publisher: string;
    name: string;
    version: string;
    license?: string;
}
