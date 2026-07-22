/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createContext } from 'react';
import { UserData } from '../../extension-registry-types';

/**
 * How the namespace detail behaves for the surrounding page: the user settings
 * pin the viewer's own membership and hide them from the member search, while
 * the admin dashboard manages everyone alike.
 */
export interface NamespaceDetailConfig {
    defaultMemberRole?: 'contributor' | 'owner';
    /** Pin the current user's own membership to Owner instead of offering role controls. */
    fixSelf?: boolean;
    /** Exclude users from the add-member search; omit to allow everyone. */
    filterUsers?: (user: UserData) => boolean;
}

export const NamespaceDetailConfigContext = createContext<NamespaceDetailConfig>({});
