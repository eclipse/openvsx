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
import GitHubIcon from '@material-ui/icons/GitHub';
import { Extension } from '../src/extension-registry-types';
import { PageSettings, Styleable } from '../src/page-settings';

export default function createPageSettings(theme: Theme): PageSettings {
    const toolbarStyle = makeStyles({
        logo: {
            width: 'auto',
            height: '40px',
            marginTop: '6px',
            marginRight: theme.spacing(2)
        }
    });
    const toolbarContent = () => <img src='/openvsx-registry.svg'
        className={toolbarStyle().logo}
        alt='Open VSX Registry' />;
    
    const footerStyle = makeStyles({
        footerBox: {
            display: 'flex',
            alignItems: 'center',
            fontSize: '1.1rem'
        }
    });
    const footerContent = () => <Link target='_blank' href='https://github.com/eclipse/openvsx'>
            <Box className={footerStyle().footerBox}>
                <GitHubIcon />&nbsp;eclipse/openvsx
            </Box>
        </Link>;

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
        toolbarContent,
        footerContent,
        searchHeader,
        reportAbuse,
        claimNamespace,
        extensionDefaultIconURL: '/default-icon.png',
        namespaceAccessInfoURL: 'https://github.com/eclipse/openvsx/wiki/Namespace-Access'
    };
}
