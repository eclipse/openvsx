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
import { Extension } from './extension-registry-types';
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
        }
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
