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
import * as readline from 'readline';
import { RegistryOptions } from './registry';

export { promisify } from 'util';

export function addEnvOptions(options: RegistryOptions): void {
    if (!options.registryUrl) {
        options.registryUrl = process.env.OVSX_REGISTRY_URL;
    }
    if (!options.pat) {
        options.pat = process.env.OVSX_PAT;
    }
    if (!options.username) {
        options.username = process.env.OVSX_USERNAME;
    }
    if (!options.password) {
        options.password = process.env.OVSX_PASSWORD;
    }
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
            fs.mkdir(path, { recursive: true }, err => {
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
        tmp.tmpName(options, (err, name) => {
            if (err)
                reject(err);
            else
                resolve(name);
        });
    });
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

export function statusError(response: http.IncomingMessage): Error {
    if (response.statusMessage)
        return new Error(`The server responded with status ${response.statusCode}: ${response.statusMessage}`);
    else
        return new Error(`The server responded with status ${response.statusCode}.`);
}

export function readFile(name: string, packagePath?: string, encoding = 'utf-8'): Promise<string> {
    return new Promise((resolve, reject) => {
        fs.readFile(
            path.join(packagePath || process.cwd(), name),
            { encoding },
            (err, content) => {
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

export function writeFile(name: string, content: string, packagePath?: string, encoding = 'utf-8'): Promise<void> {
    return new Promise((resolve, reject) => {
        fs.writeFile(
            path.join(packagePath || process.cwd(), name),
            content,
            { encoding },
            err => {
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

export function getUserInput(text: string): Promise<string> {
    return new Promise(resolve => {
        const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
        rl.question(text, answer => {
            resolve(answer);
            rl.close();
        });
    });
}

export async function getUserChoice<R extends string>(text: string, values: R[],
        defaultValue: R, lowerCase = true): Promise<R> {
    const prompt = text + '\n' + values.map(v => v === defaultValue ? `[${v}]` : v).join('/') + ': ';
    const answer = await getUserInput(prompt);
    if (!answer) {
        return defaultValue;
    }
    const lcAnswer = lowerCase ? answer.toLowerCase() : answer;
    for (const value of values) {
        if (value.startsWith(lcAnswer)) {
            return value;
        }
    }
    return defaultValue;
}
