/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as commander from 'commander';
import * as leven from 'leven';
import { createNamespace } from './create-namespace';
import { publish } from './publish';
import { handleError } from './util';
import { getExtension } from './get';

const pkg = require('../package.json');

module.exports = function (argv: string[]): void {
    const program = new commander.Command();
    program.usage('<command> [options]');
    program.option('--debug', 'include debug information on error');

    const versionCmd = program.command('version');
    versionCmd.description('Output the version number.')
        .action(() => console.log(`Eclipse Open VSX CLI version ${pkg.version}`));

    const createNamespaceCmd = program.command('create-namespace <name>');
    createNamespaceCmd.description('Create a new namespace')
        .option('-r, --registryUrl <url>', 'Use the registry API at this base URL.')
        .option('-p, --pat <token>', 'Personal access token (required).')
        .action((name: string, { registryUrl, pat }) => {
            createNamespace({ name, registryUrl, pat })
                .catch(handleError(program.debug));
        });

    const publishCmd = program.command('publish [extension.vsix]');
    publishCmd.description('Publish an extension, packaging it first if necessary.')
        .option('-r, --registryUrl <url>', 'Use the registry API at this base URL.')
        .option('-p, --pat <token>', 'Personal access token (required).')
        .option('--packagePath <path>', 'Package and publish the extension at the specified path.')
        .option('--baseContentUrl <url>', 'Prepend all relative links in README.md with this URL.')
        .option('--baseImagesUrl <url>', 'Prepend all relative image links in README.md with this URL.')
        .option('--yarn', 'Use yarn instead of npm while packing extension files.')
        .action((extensionFile: string, { registryUrl, pat, packagePath, baseContentUrl, baseImagesUrl, yarn }) => {
            if (extensionFile !== undefined && packagePath !== undefined) {
                console.error('\u274c  Please specify either a package file or a package path, but not both.\n');
                publishCmd.help();
            }
            if (extensionFile !== undefined && baseContentUrl !== undefined)
                console.warn("Ignoring option '--baseContentUrl' for prepackaged extension.");
            if (extensionFile !== undefined && baseImagesUrl !== undefined)
                console.warn("Ignoring option '--baseImagesUrl' for prepackaged extension.");
            if (extensionFile !== undefined && yarn !== undefined)
                console.warn("Ignoring option '--yarn' for prepackaged extension.");
            publish({ extensionFile, registryUrl, pat, packagePath, baseContentUrl, baseImagesUrl, yarn })
                .catch(handleError(program.debug));
        });

    const getCmd = program.command('get <namespace.extension>');
    getCmd.description('Download an extension or its metadata.')
        .option('-v, --version <version>', 'Specify an exact version or a version range.')
        .option('-r, --registryUrl <url>', 'Use the registry API at this base URL.')
        .option('-o, --output <path>', 'Save the output in the specified file or directory.')
        .option('--metadata', 'Print the extension\'s metadata instead of downloading it.')
        .action((extensionId: string, { version, registryUrl, output, metadata }) => {
            if (typeof version === 'function') // If not specified, `version` yields a function
                version = undefined;
            getExtension({ extensionId, version, registryUrl, output, metadata })
                .catch(handleError(program.debug));
        });

    program
        .command('*', '', { noHelp: true })
        .action((cmd: string) => {
            const availableCommands = program.commands.map((c: any) => c._name) as string[];
            const suggestion = availableCommands.find(c => leven(c, cmd) < c.length * 0.4);
            if (suggestion)
                console.error(`Unknown command '${cmd}', did you mean '${suggestion}'?\n`);
            else
                console.error(`Unknown command '${cmd}'`);
            program.help();
        });

    program.parse(argv);

    if (process.argv.length <= 2) {
        program.help();
    }
};
