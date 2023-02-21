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
import { Extension, NamespaceDetails } from './extension-registry-types';
import { ExtensionDetailComponent } from './pages/extension-detail/extension-detail';
import { NamespaceDetailComponent } from './pages/namespace-detail/namespace-detail';
import { Cookie } from './utils';

export interface PageSettings {
    pageTitle: string;
    themeType?: 'light' | 'dark';
    elements: {
        toolbarContent?: React.ComponentType;
        defaultMenuContent?: React.ComponentType;
        mobileMenuContent?: React.ComponentType;
        footer?: {
            content: React.ComponentType<{ expanded: boolean }>;
            props: {
                footerHeight?: number
            }
        };
        searchHeader?: React.ComponentType;
        reportAbuse?: React.ComponentType<{ extension: Extension } & Styleable>;
        claimNamespace?: React.ComponentType<{ extension: Extension } & Styleable>;
        downloadTerms?: React.ComponentType;
        additionalRoutes?: React.ComponentType;
        banner?: {
            content: React.ComponentType;
            props?: {
                dismissButton?: {
                    show?: boolean,
                    label?: string
                },
                onClose?: () => void,
                color?: 'info' | 'warning'
            },
            cookie?: Cookie
        };
        mainHeadTags?: React.ComponentType<{ pageSettings: PageSettings }>;
        extensionHeadTags?: React.ComponentType<{ extension?: Extension, params: ExtensionDetailComponent.Params, pageSettings: PageSettings }>;
        namespaceHeadTags?: React.ComponentType<{ namespaceDetails?: NamespaceDetails, params: NamespaceDetailComponent.Params, pageSettings: PageSettings }>;
    };
    urls: {
        extensionDefaultIcon: string;
        namespaceAccessInfo: string;
        publisherAgreement?: string;
    };
}

export interface Styleable {
    className: string;
}
