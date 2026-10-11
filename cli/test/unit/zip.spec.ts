/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { afterEach, describe, expect, it } from 'vitest';
import { readZip } from '../../src/zip';
import { buildZip } from './support/zip';

describe('zip', () => {

    const tmpFiles: string[] = [];

    afterEach(() => {
        tmpFiles.splice(0).forEach(file => fs.rmSync(file, { force: true }));
    });

    async function writeZip(entries: Record<string, Buffer>): Promise<string> {
        const file = path.join(os.tmpdir(), `ovsx-zip-${process.pid}-${Date.now()}-${tmpFiles.length}.zip`);
        fs.writeFileSync(file, await buildZip(entries));
        tmpFiles.push(file);
        return file;
    }

    it('readZip returns matching entries as Buffers keyed by lowercased name', async () => {
        const binary = Buffer.alloc(256 * 1024);
        for (let i = 0; i < binary.length; i++) {
            binary[i] = i % 251;
        }
        const file = await writeZip({
            'extension/Package.json': Buffer.from('{"name":"x"}'),
            'extension/data.bin': binary,
            'extension/other.txt': Buffer.from('skipped')
        });

        const result = await readZip(file, name => name !== 'extension/other.txt');

        expect([...result.keys()].sort()).toEqual(['extension/data.bin', 'extension/package.json']);
        expect(Buffer.isBuffer(result.get('extension/package.json'))).toBe(true);
        expect(result.get('extension/package.json')!.toString('utf8')).toBe('{"name":"x"}');
        expect(result.get('extension/data.bin')!.equals(binary)).toBe(true);
    });

    it('readZip returns an empty entry as an empty Buffer', async () => {
        const file = await writeZip({ 'empty.txt': Buffer.alloc(0) });

        const result = await readZip(file, () => true);

        expect(result.get('empty.txt')).toEqual(Buffer.alloc(0));
    });
});
