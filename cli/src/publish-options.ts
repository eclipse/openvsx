/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
import { RegistryOptions } from './registry-options';

export interface PublishCommonOptions extends RegistryOptions {
    /**
     * Path to the vsix file to be published. Cannot be used together with `packagePath`.
     */
    extensionFile?: string;
    /**
     * The base URL for links detected in Markdown files. Only valid with `packagePath`.
     */
    baseContentUrl?: string;
    /**
     * The base URL for images detected in Markdown files. Only valid with `packagePath`.
     */
    baseImagesUrl?: string;
    /**
     * Should use `yarn` instead of `npm`. Only valid with `packagePath`.
     */
    yarn?: boolean;
    /**
     * Mark this package as a pre-release. Only valid with `packagePath`.
     */
    preRelease?: boolean;
    /**
     * Whether to fail silently if version already exists on the marketplace
     */
    skipDuplicate?: boolean;
    /**
     * Extension version. Only valid with `packagePath`.
     */
    packageVersion?: string;
}

// Interface used by top level CLI
export interface PublishOptions extends PublishCommonOptions {

    /**
     * Target architectures.
     */
    targets?: string[];

    /**
     * Paths to the extension to be packaged and published. Cannot be used together
     * with `extensionFile`.
     */
    packagePath?: string[];

    /**
     * Whether to do dependency detection via npm or yarn
     */
    dependencies?: boolean;
}