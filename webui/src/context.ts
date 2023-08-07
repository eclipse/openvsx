/********************************************************************************
 * Copyright (c) 2019-2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createContext } from 'react';
import { PageSettings } from './page-settings';
import { ExtensionRegistryService } from './extension-registry-service';
import { UserData } from './extension-registry-types';
import { ErrorResponse } from './server-request';

export interface MainContext {
    service: ExtensionRegistryService;
    pageSettings: PageSettings;
    handleError: (err: Error | Partial<ErrorResponse>) => void;
    user?: UserData;
    updateUser: () => void;
}

// We don't include `undefined` as context value to avoid checking the value in all components
export const MainContext = createContext<MainContext>(undefined!);
