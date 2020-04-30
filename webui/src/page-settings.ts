/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Extension } from "./extension-registry-types";

export interface PageSettings {
    pageTitle: string;
    toolbarContent?: React.FunctionComponent;
    footerContent?: React.FunctionComponent;
    searchHeader?: React.FunctionComponent,
    reportAbuse?: React.FunctionComponent<{ extension: Extension } & Styleable>;
    claimNamespace?: React.FunctionComponent<{ extension: Extension } & Styleable>;
    additionalRoutes?: React.FunctionComponent,
    extensionDefaultIconURL?: string;
    namespaceAccessInfoURL?: string;
    themeType?: 'light' | 'dark';
}

export interface Styleable {
    className: string;
}
