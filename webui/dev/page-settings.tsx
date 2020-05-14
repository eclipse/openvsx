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
import { Link, Typography, Theme, Box } from '@material-ui/core';
import { Link as RouteLink, Route } from 'react-router-dom';
import GitHubIcon from '@material-ui/icons/GitHub';
import { Extension } from '../src/extension-registry-types';
import { PageSettings, Styleable } from '../src/page-settings';
import { ExtensionListRoutes } from '../src/pages/extension-list/extension-list-container';
import About from './about';
import OpenVSXLogo from './openvsx-registry-logo';

export default function createPageSettings(theme: Theme, prefersDarkMode: boolean): PageSettings {
    const toolbarStyle = makeStyles({
        logo: {
            width: 'auto',
            height: '40px',
            marginTop: '7px'
        }
    });
    const toolbarContent = () => <RouteLink
            to={ExtensionListRoutes.MAIN} aria-label={`Home - Open VSX Registry`}>
            <OpenVSXLogo prefersDarkMode={prefersDarkMode} className={toolbarStyle().logo}/>
        </RouteLink>;

    const footerStyle = makeStyles({
        wrapper: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            [theme.breakpoints.down('sm')]: {
                flexDirection: 'column'
            }
        },
        repositoryLink: {
            display: 'flex',
            alignItems: 'center',
            fontSize: '1.1rem'
        },
        legalLink: {
            marginLeft: theme.spacing(3),
            textDecoration: 'none',
            color: theme.palette.primary.main,
            '&:hover': {
                textDecoration: 'underline'
            },
            [theme.breakpoints.down('sm')]: {
                marginLeft: theme.spacing(1.5),
                marginTop: theme.spacing(2)
            }
        }
    });
    const footerContent = () => <Box className={footerStyle().wrapper}>
            <Link target='_blank' href='https://github.com/eclipse/openvsx' className={footerStyle().repositoryLink}>
                <GitHubIcon />&nbsp;eclipse/openvsx
            </Link>
            <Box display='flex'>
                <RouteLink to='/about' className={footerStyle().legalLink}>
                    About This Service
                </RouteLink>
                <Link href='https://www.example.com/terms' className={footerStyle().legalLink}>
                    Terms of Use
                </Link>
            </Box>
        </Box>;

    const searchStyle = makeStyles({
        typography: {
            marginBottom: theme.spacing(2),
            fontWeight: theme.typography.fontWeightLight,
            letterSpacing: 4,
            textAlign: 'center'
        }
    });
    const searchHeader = () => <Typography variant='h4' classes={{ root: searchStyle().typography }}>
            Extensions for VS Code Compatible Editors
        </Typography>;

    const additionalRoutes = () => <Route path='/about' render={() => <About />} />

    const reportAbuseText = encodeURIComponent('<Please describe the issue>');
    const extensionURL = (extension: Extension) => encodeURIComponent(
        `${location.protocol}//${location.hostname}/extension/${extension.namespace}/${extension.name}`);
    const reportAbuse: React.FunctionComponent<{ extension: Extension } & Styleable> = ({ extension, className }) => <Link
            href={`mailto:abuse@example.com?subject=Report%20Abuse%20-%20${extension.namespace}.${extension.name}&Body=${reportAbuseText}%0A%0A${extensionURL(extension)}`}
            variant='body2' color='secondary' className={className} >
            Report Abuse
        </Link>;

    const claimNamespace: React.FunctionComponent<{ extension: Extension } & Styleable> = ({ extension, className }) => <Link
            href={`https://github.com/myorg/myrepo/issues/new?title=Claiming%20ownership%20of%20${extension.namespace}`}
            target='_blank' variant='body2' color='secondary' className={className} >
            Claim Ownership
        </Link>;

    return {
        pageTitle: 'Open VSX Registry',
        themeType: prefersDarkMode ? 'dark' : 'light',
        toolbarContent,
        footerContent,
        searchHeader,
        additionalRoutes,
        reportAbuse,
        claimNamespace,
        metrics: {
            maxFooterHeight: 85
        },
        urls: {
            extensionDefaultIcon: '/default-icon.png',
            namespaceAccessInfo: 'https://github.com/eclipse/openvsx/wiki/Namespace-Access'
        }
    };
}
