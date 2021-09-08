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
    program.usage('<command> [options]')
        .option('-r, --registryUrl <url>', 'Use the registry API at this base URL.')
        .option('-p, --pat <token>', 'Personal access token.')
        .option('--debug', 'Include debug information on error')
        .version(pkg.version, '-V, --version', 'Print the Eclipse Open VSX CLI version');

    const createNamespaceCmd = program.command('create-namespace <name>');
    createNamespaceCmd.description('Create a new namespace')
        .action((name: string) => {
            const { registryUrl, pat } = program.opts();
            createNamespace({ name, registryUrl, pat })
                .catch(handleError(program.debug));
        });

    const publishCmd = program.command('publish [extension.vsix]');
    publishCmd.description('Publish an extension, packaging it first if necessary.')
        .option('--packagePath <path>', 'Package and publish the extension at the specified path.')
        .option('--baseContentUrl <url>', 'Prepend all relative links in README.md with this URL.')
        .option('--baseImagesUrl <url>', 'Prepend all relative image links in README.md with this URL.')
        .option('--yarn', 'Use yarn instead of npm while packing extension files.')
        .action((extensionFile: string, { packagePath, baseContentUrl, baseImagesUrl, yarn }) => {
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
            const { registryUrl, pat } = program.opts();
            publish({ extensionFile, registryUrl, pat, packagePath, baseContentUrl, baseImagesUrl, yarn })
                .catch(handleError(program.debug,
                    'See the documentation for more information:\n'
                    + 'https://github.com/eclipse/openvsx/wiki/Publishing-Extensions'
                ));
        });

    const getCmd = program.command('get <namespace.extension>');
    getCmd.description('Download an extension or its metadata.')
        .option('-v, --versionRange <version>', 'Specify an exact version or a version range.')
        .option('-o, --output <path>', 'Save the output in the specified file or directory.')
        .option('--metadata', 'Print the extension\'s metadata instead of downloading it.')
        .action((extensionId: string, { versionRange, output, metadata }) => {
            const { registryUrl } = program.opts();
            getExtension({ extensionId, version: versionRange, registryUrl, output, metadata })
                .catch(handleError(program.debug));
        });

    program
        .command('*', '', { noHelp: true })
        .action((cmd: commander.Command) => {
            const availableCommands = program.commands.map((c: any) => c._name) as string[];
            const actualCommand = cmd.args[0];
            if (actualCommand) {
                const suggestion = availableCommands.find(c => leven(c, actualCommand) < c.length * 0.4);
                if (suggestion)
                    console.error(`Unknown command '${actualCommand}', did you mean '${suggestion}'?\n`);
                else
                    console.error(`Unknown command '${actualCommand}'.\n`);
            } else {
                console.error('Unknown command.');
            }
            program.help();
        });

    program.parse(argv);

    if (process.argv.length <= 2) {
        program.help();
    }
};
