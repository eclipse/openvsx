/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

const { Registry, Response, Extension } = require("../lib/registry");
const { makeDirs } = require('../lib/util');
const path = require('path');

async function test() {
    const registry = new Registry();
    const localReg = new Registry({url: 'http://localhost:8080'})
    const json = await registry.getJson(new URL('https://open-vsx.org/api/-/search?size=100'));
    if (!json.error) {
        json.extensions.forEach(async ext => {
            const meta = await registry.getMetadata(ext.namespace, ext.name);
            if(meta.error) {
                console.log(meta.error);
            }
            const file = await download(registry, meta);
            const ns = await localReg.createNamespace(meta.namespace, 'super_token');
            if(ns.error) {
                console.log(ns.error);
            }
            const published = await localReg.publish(file, 'super_token');
            if(published.error) {
                console.log(published.error);
            }
        })
    }
}

async function download(registry, extension) {
    const downloadUrl = extension.files.download;
    if (!downloadUrl) {
        throw new Error(`Extension ${extension.namespace}.${extension.name} does not provide a download URL.`);
    }
    const fileNameIndex = downloadUrl.lastIndexOf('/');
    const fileName = downloadUrl.substring(fileNameIndex + 1);
    let filePath;
    filePath = path.resolve(process.cwd(), 'downloads', fileName);
    await makeDirs(path.dirname(filePath));
    console.log(`Downloading ${extension.namespace}.${extension.name} v${extension.version} to ${filePath}`);
    await registry.download(filePath, new URL(downloadUrl));
    return filePath;
}

test();