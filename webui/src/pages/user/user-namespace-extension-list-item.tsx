/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { useContext, FunctionComponent } from 'react';
import { Extension } from '../../extension-registry-types';
import { Paper, Typography, Box, makeStyles } from '@material-ui/core';
import { Link as RouteLink } from 'react-router-dom';
import { createRoute } from '../../utils';
import { Timestamp } from '../../components/timestamp';
import { ExtensionDetailRoutes } from '../extension-detail/extension-detail';
import { PageSettingsContext } from '../../default/default-app';

const itemStyles = makeStyles(theme => ({
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
        marginRight: theme.spacing(2),
        height: '3rem',
        maxWidth: '4rem',
    },
    paragraph: {
        display: 'flex',
        justifyContent: 'space-between',
    }
}));

export const UserNamespaceExtensionListItem: FunctionComponent<UserNamespaceExtensionListItemProps> = props => {
    const pageSettings = useContext(PageSettingsContext);
    const { extension } = props;
    const classes = itemStyles();
    const route = extension && createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]) || '';
    return (
        extension ? (
            <RouteLink to={route} className={classes.link}>
                <Paper className={classes.paper}>
                    <img
                        src={extension.files.icon || (pageSettings && pageSettings.urls.extensionDefaultIcon) || ''}
                        alt={extension.displayName || extension.name}
                        className={classes.extensionLogo}
                    />
                    <div className={classes.text}>
                        <Typography variant='h6' noWrap style={{ fontSize: '1.15rem' }}>
                            {extension.displayName || extension.name}
                        </Typography>
                        <Box className={classes.paragraph} mt={1}>
                            <span>Latest Version:</span>
                            <span>{extension.version}</span>
                        </Box>
                        {
                            extension.timestamp ?
                                <Box className={classes.paragraph} mt={0.25}>
                                    <span>Released:</span>
                                    <Timestamp value={extension.timestamp} />
                                </Box>
                                : null
                        }
                    </div>
                </Paper>
            </RouteLink>
        )
            : null
    );
};

export interface UserNamespaceExtensionListItemProps {
    extension: Extension;
}