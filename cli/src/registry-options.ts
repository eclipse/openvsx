/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { Logger } from './logger';

export interface RegistryOptions {
    /**
     * The base URL of the registry API.
     */
    registryUrl?: string;
    /**
     * Where to write progress messages. Defaults to the console.
     */
    log?: Logger;
    /**
     * Whether asking the user for input is allowed, e.g. for a missing access token. Defaults to
     * `true`, except for the programmatic API in {@link publishVSIX} and {@link createVSIX}, which
     * never prompts unless this is set explicitly.
     */
    interactive?: boolean;
    /**
     * Personal access token.
     */
    pat?: string;
    /**
     * User name for basic authentication.
     */
    username?: string;
    /**
     * Password for basic authentication.
     */
    password?: string;
    /**
     * Maximal request body size for creating namespaces.
     */
    maxNamespaceSize?: number;
    /**
     * Maximal request body size for publishing.
     */
    maxPublishSize?: number;
}
