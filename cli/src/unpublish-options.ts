/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { RegistryOptions } from './registry-options';

export interface UnpublishOptions extends RegistryOptions {
    /**
     * Identifier in the form `namespace.extension` or `namespace/extension`.
     * Read from the `package.json` in the current directory if omitted.
     */
    extensionId?: string;
    /**
     * Versions to delete. All versions are deleted if omitted.
     */
    versions?: string[];
    /**
     * Target platforms to delete. All target platforms of the selected versions
     * are deleted if omitted. Can only be used together with `versions`.
     */
    targets?: string[];
    /**
     * Delete without asking for confirmation.
     */
    force?: boolean;
}
