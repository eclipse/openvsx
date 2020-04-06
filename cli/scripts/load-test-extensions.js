/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

//@ts-check
const { Registry, DEFAULT_URL } = require('../lib/registry');
const { makeDirs } = require('../lib/util');
const path = require('path');
const fs = require('fs');

let searchSize = 100;
if (process.argv.length >= 3) {
    searchSize = parseInt(process.argv[2], 10);
}

let accessToken = 'super_token';
if (process.argv.length >= 4) {
    accessToken = process.argv[3];
}

async function loadTestExtensions() {
    const publicReg = new Registry();
    const localReg = new Registry({url: 'http://localhost:8080'});
    /** @type {{ extensions: import('../lib/registry').Extension[] } & import('../lib/registry').Response} */
    const search = await publicReg.getJson(new URL(`${DEFAULT_URL}/api/-/search?size=${searchSize}`));
    if (search.error) {
        console.error(search.error);
        process.exit(1);
    }
    console.log(`Found ${search.extensions.length} extensions in ${DEFAULT_URL}`);
    for (const ext of search.extensions) {
        const meta = await publicReg.getMetadata(ext.namespace, ext.name);
        if (meta.error) {
            console.error(`\u274c  ${meta.error}`);
            continue;
        }
        const fileName = await download(publicReg, meta);
        const nsResult = await localReg.createNamespace(meta.namespace, accessToken);
        if (nsResult.error && !nsResult.error.startsWith('Namespace already exists')) {
            console.error(nsResult.error);
        } else if (nsResult.success) {
            console.log(nsResult.success);
        }
        const published = await localReg.publish(fileName, accessToken);
        if (published.error && !published.error.endsWith('is already published.')) {
            console.error(`\u274c  ${published.error}`);
        } else if (published.namespace && published.name) {
            console.log(`\u2713  Published ${published.namespace}.${published.name}@${published.version}`);
        }
    }
}

/**
 * @param {Registry} registry 
 * @param {import('../lib/registry').Extension} extension 
 */
async function download(registry, extension) {
    const downloadUrl = extension.files.download;
    const fileNameIndex = downloadUrl.lastIndexOf('/');
    const fileName = downloadUrl.substring(fileNameIndex + 1);
    const filePath = path.resolve(__dirname, 'downloads', fileName);
    if (fs.existsSync(filePath)) {
        return filePath;
    }
    await makeDirs(path.dirname(filePath));
    console.log(`Downloading ${extension.namespace}.${extension.name}@${extension.version} to ${filePath}`);
    await registry.download(filePath, new URL(downloadUrl));
    return filePath;
}

loadTestExtensions();
