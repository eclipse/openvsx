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

export { promisify } from 'util';

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

export function handleError(debug?: boolean): (reason: any) => void {
    return reason => {
        if (reason instanceof Error && !debug) {
            console.error(`\u274c  ${reason.message}`);
        } else if (typeof reason === 'string') {
            console.error(`\u274c  ${reason}`);
        } else if (reason !== undefined) {
            console.error(reason);
        } else {
            console.error('An unknown error occurred.');
        }
        process.exit(1);
    };
}

export function statusError(response: http.IncomingMessage): Error {
    if (response.statusMessage)
        return new Error(`The server responded with status ${response.statusCode}: ${response.statusMessage}`);
    else
        return new Error(`The server responded with status ${response.statusCode}.`);
}

export function readManifest(packagePath?: string): Promise<Manifest> {
    return new Promise((resolve, reject) => {
        fs.readFile(
            path.join(packagePath || '.', 'package.json'),
            { encoding: 'utf-8' },
            (readErr, data) => {
                if (readErr) {
                    reject(readErr);
                } else {
                    try {
                        resolve(JSON.parse(data));
                    } catch (jsonErr) {
                        reject(jsonErr);
                    }
                }
            }
        );
    });
}

export function writeManifest(manifest: Manifest, packagePath?: string): Promise<void> {
    return new Promise((resolve, reject) => {
        const content = JSON.stringify(manifest, null, 4);
        fs.writeFile(
            path.join(packagePath || '.', 'package.json'),
            content,
            { encoding: 'utf-8' },
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

interface Manifest {
    publisher: string;
    name: string;
    version: string;
    license?: string;
}

export function booleanQuestion(text: string, defaultValue = false): Promise<boolean> {
    return new Promise(resolve => {
        const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
        rl.question(`${text}\n${defaultValue ? '[yes]/no' : 'yes/[no]'}: `, answer => {
            resolve(!answer && defaultValue || !!answer && 'yes'.startsWith(answer.toLowerCase()));
            rl.close();
        });
    });
}
