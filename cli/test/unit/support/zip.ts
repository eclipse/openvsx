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

import * as yazl from 'yazl';

/**
 * Builds a zip in memory from entry name to content, for the specs that need a real package to hand
 * to code that reads one - a `.vsix` is a zip with an `extension/package.json` in it.
 */
export function buildZip(entries: Record<string, Buffer>): Promise<Buffer> {
    return new Promise((resolve, reject) => {
        const zipFile = new yazl.ZipFile();
        for (const [name, content] of Object.entries(entries)) {
            zipFile.addBuffer(content, name);
        }
        const chunks: Buffer[] = [];
        zipFile.outputStream.on('data', chunk => chunks.push(chunk));
        zipFile.outputStream.on('end', () => resolve(Buffer.concat(chunks)));
        zipFile.outputStream.on('error', reject);
        zipFile.end();
    });
}
