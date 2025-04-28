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
const { RateLimiter } = require('limiter');

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
    const publicLimiter = new RateLimiter({ tokensPerInterval: 15, interval: 'second' });
    const localReg = new Registry({ registryUrl: process.env.OVSX_REGISTRY_URL ?? 'http://localhost:8080' });
    const localLimiter = new RateLimiter({ tokensPerInterval: 15, interval: 'second' });
    /** @type {{ extensions: import('../lib/registry').Extension[] } & import('../lib/registry').Response} */
    await publicLimiter.removeTokens(1);
    const search = await publicReg.getJson(new URL(`${DEFAULT_URL}/api/-/search?size=${searchSize}`));
    if (search.error) {
        console.error(search.error);
        process.exit(1);
    }
    console.log(`Found ${search.extensions.length} extensions in ${DEFAULT_URL}`);
    for (const ext of search.extensions) {
        await publicLimiter.removeTokens(1);
        const meta = await publicReg.getMetadata(ext.namespace, ext.name);
        if (meta.error) {
            console.error(`\u274c  ${meta.error}`);
            continue;
        }
        const fileName = await download(publicReg, meta, publicLimiter);
        await createNamespace(localReg, meta, localLimiter);
        await publishExtension(localReg, fileName, localLimiter);
    }
}

/**
 * @param {Registry} registry 
 * @param {import('../lib/registry').Extension} extension 
 * @param {RateLimiter} rateLimiter
 */
async function download(registry, extension, rateLimiter) {
    const downloadUrl = extension.files.download;
    const fileNameIndex = downloadUrl.lastIndexOf('/');
    const fileName = downloadUrl.substring(fileNameIndex + 1);
    const filePath = path.resolve(__dirname, 'downloads', fileName);
    if (fs.existsSync(filePath)) {
        return filePath;
    }
    await makeDirs(path.dirname(filePath));
    console.log(`Downloading ${extension.namespace}.${extension.name}@${extension.version} to ${filePath}`);
    await rateLimiter.removeTokens(1);
    await registry.download(filePath, new URL(downloadUrl));
    return filePath;
}

/**
 * @param {Registry} registry 
 * @param {import('../lib/registry').Extension} extension 
 * @param {RateLimiter} rateLimiter
 */
async function createNamespace(registry, extension, rateLimiter) {
    try {
        await rateLimiter.removeTokens(1);
        const nsResult = await registry.createNamespace(extension.namespace, accessToken);
        console.log(nsResult.success);
    } catch (error) {
        if (!error.message.startsWith('Namespace already exists')) {
            console.error(error);
            process.exit(1);
        }
    }
}

/**
 * @param {Registry} registry 
 * @param {string} fileName 
 * @param {RateLimiter} rateLimiter
 */
async function publishExtension(registry, fileName, rateLimiter) {
    try {
        await rateLimiter.removeTokens(1);
        const published = await registry.publish(fileName, accessToken);
        if (published.namespace && published.name) {
            console.log(`\u2713  Published ${published.namespace}.${published.name}@${published.version}`);
        }
    } catch (error) {
        if (!error.message.endsWith('is already published.')) {
            console.error(`\u274c  ${error}`);
        }
    }
}

loadTestExtensions();
