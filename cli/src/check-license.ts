/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as fs from 'fs';
import * as path from 'path';
import { readManifest, writeManifest, Manifest, getUserInput, getUserChoice, writeFile } from './util';

export async function checkLicense(packagePath?: string): Promise<void> {
    if (await hasLicenseFile(packagePath)) {
        // The extension has a LICENSE file that can be packaged
        return;
    }
    const manifest = await readManifest(packagePath);
    if (!manifest.publisher) {
        throw new Error("Missing required field 'publisher'.");
    }
    if (!manifest.name) {
        throw new Error("Missing required field 'name'.");
    }
    if (manifest.license) {
        // The extension has a license identifier or a pointer to an alternative LICENSE file
        return;
    }
    console.log('Extension ' + manifest.publisher + '.' + manifest.name + ' has no license. All Open VSX '
        + 'Registry Content Offerings must be licensed. You may choose to publish this extension under '
        + 'the MIT License (https://opensource.org/licenses/MIT). Please note you are responsible to '
        + 'ensure that you have the necessary rights to permit this extension to be made available under '
        + 'the MIT license and for compliance with that license.');
    let answer: 'yes' | 'help' | 'no';
    do {
        console.log();
        answer = await getUserChoice('Would you like to publish your extension '
            + manifest.publisher + '.' + manifest.name
            + ' under the MIT license?',
            ['yes', 'help', 'no'], 'no');
        switch (answer) {
            case 'yes':
                await useMITLicense(manifest, packagePath);
                break;
            case 'help':
                console.log('If you select "yes" your extension will be published under the MIT License. '
                    + 'You must enter the Copyright Year and Copyright Holder information. This information '
                    + 'along with the text of the MIT License will be written to a LICENSE file and '
                    + 'packaged with the uploaded extension.\n');
                console.log(MIT_LICENSE_TEXT);
                break;
            case 'no':
                throw new Error('This extension cannot be accepted because it has no license.');
        }
    } while (answer === 'help');
}

async function useMITLicense(manifest: Manifest, packagePath?: string) {
    console.log('Please enter a value for Copyright Year and Copyright Holder.\n'
        + 'Example: "Copyright 2020 John Doe"\n');
    const copyright = await getUserInput('Copyright ');
    if (!copyright) {
        throw new Error('A copyright declaration is necessary for the MIT license.');
    }
    manifest.license = 'MIT';
    await writeManifest(manifest, packagePath);
    const license = MIT_LICENSE_TEXT.replace('<YEAR> <COPYRIGHT HOLDER>', copyright);
    await writeFile('LICENSE', license, packagePath);
    console.log('LICENSE file has been written. Please commit it to the source repository.');
}

const LICENSE_FILE_NAMES = ['LICENSE.md', 'LICENSE', 'LICENSE.txt'];

async function hasLicenseFile(packagePath?: string): Promise<boolean> {
    for (const fileName of LICENSE_FILE_NAMES) {
        const promise = new Promise(resolve => fs.access(
            path.join(packagePath || '.', fileName),
            err => resolve(!err)
        ));
        if (await promise) {
            return true;
        }
    }
    return false;
}

const MIT_LICENSE_TEXT: string = `Copyright <YEAR> <COPYRIGHT HOLDER>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.`;
