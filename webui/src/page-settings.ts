/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { ComponentType, ReactNode } from 'react';
import { SxProps, Theme } from '@mui/material/styles';
import { Extension, NamespaceDetails } from './extension-registry-types';
import { Cookie } from './utils';

export interface PageSettings {
    pageTitle: string;
    themeType?: 'light' | 'dark';
    elements: {
        toolbarContent?: ComponentType;
        defaultMenuContent?: ComponentType;
        mobileMenuContent?: ComponentType;
        footer?: {
            content: ComponentType<{ expanded: boolean }>;
            props: {
                footerHeight?: number
            }
        };
        searchHeader?: ComponentType;
        reportAbuse?: ComponentType<{ extension: Extension, sx?: SxProps<Theme> }>;
        claimNamespace?: ComponentType<{ extension: Extension, sx?: SxProps<Theme> }>;
        downloadTerms?: ComponentType;
        additionalRoutes?: ReactNode;
        banner?: {
            content: ComponentType;
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
        mainHeadTags?: ComponentType<{ pageSettings: PageSettings }>;
        extensionHeadTags?: ComponentType<{ extension?: Extension, pageSettings: PageSettings }>;
        namespaceHeadTags?: ComponentType<{ namespaceDetails?: NamespaceDetails, name: string, pageSettings: PageSettings }>;
    };
    urls: {
        extensionDefaultIcon: string;
        namespaceAccessInfo: string;
        publisherAgreement?: string;
    };
}
