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
import { makeStyles } from '@material-ui/styles';
import { Link, Typography, Theme } from '@material-ui/core';
import { Link as RouteLink } from 'react-router-dom';
import GitHubIcon from '@material-ui/icons/GitHub';
import { PageSettings } from '../page-settings';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-container';
import OpenVSXLogo from './openvsx-registry-logo';

export default function createPageSettings(theme: Theme, prefersDarkMode: boolean): PageSettings {
    const toolbarStyle = makeStyles({
        logo: {
            width: 'auto',
            height: '40px',
            marginTop: '7px'
        }
    });
    const toolbarContent = () =>
        <RouteLink to={ExtensionListRoutes.MAIN} aria-label={`Home - Open VSX Registry`}>
            <OpenVSXLogo prefersDarkMode={prefersDarkMode} className={toolbarStyle().logo}/>
        </RouteLink>;

    const footerStyle = makeStyles({
        repositoryLink: {
            display: 'flex',
            alignItems: 'center',
            fontSize: '1.1rem'
        }
    });
    const footerContent = () =>
        <Link target='_blank' href='https://github.com/eclipse/openvsx' className={footerStyle().repositoryLink}>
            <GitHubIcon />&nbsp;eclipse/openvsx
        </Link>;

    const searchStyle = makeStyles({
        typography: {
            marginBottom: theme.spacing(2),
            fontWeight: theme.typography.fontWeightLight,
            letterSpacing: 4,
            textAlign: 'center'
        }
    });
    const searchHeader = () =>
        <Typography variant='h4' classes={{ root: searchStyle().typography }}>
            Extensions for VS Code Compatible Editors
        </Typography>;

    return {
        pageTitle: 'Open VSX Registry',
        themeType: prefersDarkMode ? 'dark' : 'light',
        elements: {
            toolbarContent,
            footerContent,
            searchHeader
        },
        metrics: {
            maxFooterHeight: 85
        },
        urls: {
            extensionDefaultIcon: '/default-icon.png',
            namespaceAccessInfo: 'https://github.com/eclipse/openvsx/wiki/Namespace-Access'
        }
    };
}
