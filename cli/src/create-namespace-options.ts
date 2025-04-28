/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { RegistryOptions } from './registry-options';

export interface CreateNamespaceOptions extends RegistryOptions {
    /**
     * Name of the new namespace.
     */
    name?: string
}
