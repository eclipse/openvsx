/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

// Everything listed here is part of the public API of this package and therefore a compatibility
// promise. The exports are named on purpose: with `export *` every symbol a module below adds would
// silently become public, even when it is only meant to be shared between two modules of this
// package.

// Packaging and publishing.
export { createVSIX, publishVSIX, CreateVSIXOptions, PublishVSIXOptions } from './api';
export { publish } from './publish';
export { PublishCommonOptions, PublishOptions } from './publish-options';

// Namespaces and access tokens.
export { createNamespace } from './create-namespace';
export { CreateNamespaceOptions } from './create-namespace-options';
export { verifyPat } from './verify-pat';
export { VerifyPatOptions } from './verify-pat-options';
export { LoginOptions } from './login-options';

// Downloading extensions.
export { getExtension } from './get';
export { GetOptions } from './get-options';

// The registry API client the commands above are built on.
export {
    Registry, DEFAULT_URL, DEFAULT_NAMESPACE_SIZE, DEFAULT_PUBLISH_SIZE,
    Response, Extension, UserData, Badge, ExtensionReference, ErrorResponse
} from './registry';
export { RegistryOptions } from './registry-options';

// Where the commands report their progress to.
export { Logger, consoleLogger, silentLogger } from './logger';

// Helpers for inspecting an extension before publishing it.
export { isLicenseOk } from './check-license';
export { validateManifest, readManifest } from './util';
