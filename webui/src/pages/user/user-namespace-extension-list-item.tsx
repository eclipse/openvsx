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
import { MainContext } from '../../context';
import { createRoute } from '../../utils';
import { Timestamp } from '../../components/timestamp';
import { ExtensionDetailRoutes } from '../extension-detail/extension-detail';

const itemStyles = makeStyles(theme => ({
    link: {
        textDecoration: 'none',
    },
    paper: {
        display: 'flex',
        alignItems: 'center',
        padding: theme.spacing(1),
    },
    inactive: {
        opacity: 0.75
    },
    textContent: {
        flex: '1',
        overflow: 'hidden'
    },
    extensionLogo: {
        flex: '0 0 15%',
        display: 'block',
        marginRight: theme.spacing(2),
        width: '3rem',
        maxHeight: '4rem',
    },
    extensionTitle: {
        fontSize: '1.15rem'
    },
    paragraph: {
        display: 'flex',
        justifyContent: 'space-between',
    },
    noOverflow: {
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        marginLeft: theme.spacing(0.5)
    }
}));

export const UserNamespaceExtensionListItem: FunctionComponent<UserNamespaceExtensionListItemProps> = props => {
    const { pageSettings } = useContext(MainContext);
    const { extension } = props;
    const classes = itemStyles();
    const route = extension && createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]) || '';
    const inactive = extension.active === false;
    return (
        extension ? (
            <RouteLink to={route} className={classes.link}>
                <Paper
                    title={`${extension.namespace}.${extension.name} ${extension.version} ${inactive ? '(deactivated)' : ''}`}
                    className={inactive ? `${classes.paper} ${classes.inactive}` : classes.paper} >
                    <img
                        src={extension.files.icon || (pageSettings && pageSettings.urls.extensionDefaultIcon) || ''}
                        alt={extension.displayName || extension.name}
                        className={classes.extensionLogo}
                    />
                    <div className={classes.textContent}>
                        <Typography variant='h6' noWrap className={classes.extensionTitle}>
                            {extension.displayName || extension.name}
                        </Typography>
                        <Box className={classes.paragraph} mt={1}>
                            <span>Version:</span>
                            <span className={classes.noOverflow}>{extension.version}</span>
                        </Box>
                        {
                            inactive ?
                            <Box mt={0.25}>
                                Deactivated
                            </Box>
                            : extension.timestamp ?
                            <Box className={classes.paragraph} mt={0.25}>
                                <span>Published:</span>
                                <Timestamp
                                    value={extension.timestamp}
                                    className={classes.noOverflow} />
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