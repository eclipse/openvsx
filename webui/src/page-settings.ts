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
    themeType?: 'light' | 'dark';
    elements: {
        toolbarContent?: React.ComponentType;
        defaultMenuContent?: React.ComponentType;
        mobileMenuContent?: React.ComponentType;
        footerContent?: React.ComponentType;
        searchHeader?: React.ComponentType;
        reportAbuse?: React.ComponentType<{ extension: Extension } & Styleable>;
        claimNamespace?: React.ComponentType<{ extension: Extension } & Styleable>;
        additionalRoutes?: React.ComponentType;
    };
    metrics?: {
        maxFooterHeight?: number;
    };
    urls: {
        extensionDefaultIcon: string;
        namespaceAccessInfo: string;
        publisherAgreement?: string;
    }
}

export interface Styleable {
    className: string;
}
