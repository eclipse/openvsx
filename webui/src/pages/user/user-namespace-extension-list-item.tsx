/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { useContext, FunctionComponent, useState, useEffect } from 'react';
import { Extension } from '../../extension-registry-types';
import { Paper, Typography, Box, styled } from '@mui/material';
import { Link as RouteLink } from 'react-router-dom';
import { MainContext } from '../../context';
import { createRoute } from '../../utils';
import { Timestamp } from '../../components/timestamp';
import { ExtensionDetailRoutes } from '../extension-detail/extension-detail';

const noOverflow = {
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    ml: 0.5
};

const Paragraph = styled(Box)({
    display: 'flex',
    justifyContent: 'space-between',
});

export const UserNamespaceExtensionListItem: FunctionComponent<UserNamespaceExtensionListItemProps> = props => {
    const { pageSettings, service } = useContext(MainContext);
    const [icon, setIcon] = useState<string | undefined>(undefined);
    const { extension } = props;
    const route = extension && createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]) || '';
    const inactive = extension.active === false;
    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);
    useEffect(() => {
        if (icon) {
            URL.revokeObjectURL(icon);
        }

        service.getExtensionIcon(abortController, extension).then(setIcon);
    }, [extension]);

    return (
        extension ? (
            <RouteLink to={route} style={{ textDecoration: 'none' }}>
                <Paper
                    elevation={3}
                    title={`${extension.namespace}.${extension.name} ${extension.version} ${inactive ? '(deactivated)' : ''}`}
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        p: 1,
                        opacity: (inactive ? 0.75 : 1)
                    }}>
                    <Box
                        component='img'
                        src={icon || (pageSettings && pageSettings.urls.extensionDefaultIcon) || ''}
                        alt={extension.displayName || extension.name}
                        sx={{
                            flex: '0 0 15%',
                            display: 'block',
                            mr: 2,
                            width: '3rem',
                            maxHeight: '4rem',
                        }}
                    />
                    <Box component='div' sx={{ flex: '1', overflow: 'hidden' }}>
                        <Typography variant='h6' noWrap sx={{ fontSize: '1.15rem' }}>
                            {extension.displayName || extension.name}
                        </Typography>
                        <Paragraph mt={1}>
                            <span>Version:</span>
                            <Box component='span' sx={noOverflow}>{extension.version}</Box>
                        </Paragraph>
                        {
                            inactive ?
                            <Box mt={0.25}>
                                Deactivated
                            </Box>
                            : extension.timestamp ?
                            <Paragraph mt={0.25}>
                                <span>Published:</span>
                                <Timestamp
                                    value={extension.timestamp}
                                    sx={noOverflow} />
                            </Paragraph>
                            : null
                        }
                    </Box>
                </Paper>
            </RouteLink>
        )
            : null
    );
};

export interface UserNamespaceExtensionListItemProps {
    extension: Extension;
}