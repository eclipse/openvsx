/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { RegistryOptions } from "./registry-options";

export interface GetOptions extends RegistryOptions {
    /**
     * Identifier in the form `namespace.extension` or `namespace/extension`.
     */
    extensionId: string;
    /**
     * Target platform.
     */
    target?: string;
    /**
     * An exact version or version range.
     */
    version?: string;
    /**
     * Save the output in the specified file or directory.
     */
    output?: string;
    /**
     * Print the extension's metadata instead of downloading it.
     */
    metadata?: boolean;
}
