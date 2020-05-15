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
import { Extension } from '../../extension-registry-types';
import { Paper, Typography, Theme, createStyles, WithStyles, withStyles } from '@material-ui/core';
import { createRoute } from '../../utils';
import { ExtensionDetailRoutes } from '../extension-detail/extension-detail';
import { Link as RouteLink } from 'react-router-dom';
import { PageSettings } from '../../page-settings';

const itemStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
    },
    paper: {
        display: 'flex',
        alignItems: 'center',
        padding: theme.spacing(1),
    },
    text: {
        flex: '1',
        overflow: 'hidden'
    },
    extensionLogo: {
        flex: '0 0 15%',
        display: 'block',
        marginRight: '.5rem',
        height: '3rem',
        maxWidth: '4rem',
    },
    p: {
        display: 'flex',
        justifyContent: 'space-between',
    }
});

const UserNamespaceExtensionListItemComponent = ({ extension, classes, pageSettings }: UserNamespaceExtensionListItemComponent.Props) => {
    const route = extension && createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]) || '';
    return (
        extension ? (
            <RouteLink to={route} className={classes.link}>
                <Paper className={classes.paper}>
                    <img
                        src={extension.files.icon || pageSettings.urls.extensionDefaultIcon}
                        alt={extension.displayName || extension.name}
                        className={classes.extensionLogo}
                    />
                    <div className={classes.text}>
                        <Typography variant='h6' noWrap style={{ fontSize: '1.15rem' }}>
                            {extension.displayName || extension.name}
                        </Typography>
                        <p className={classes.p}>
                            <span>Latest Version:</span>
                            <span>{extension.version}</span>
                        </p>
                        <p className={classes.p}>
                            <span>Released on:</span>
                            <span>{(new Date(extension.timestamp || '')).toLocaleDateString('en-GB', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
                        </p>
                    </div>
                </Paper>
            </RouteLink>
        )
            : null
    );
};

export namespace UserNamespaceExtensionListItemComponent {
    export interface Props extends WithStyles<typeof itemStyles> {
        extension: Extension;
        pageSettings: PageSettings;
    }
}

export const UserNamespaceExtensionListItem = withStyles(itemStyles)(UserNamespaceExtensionListItemComponent);
