/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import type { ReactNode } from 'react';
import { FunctionComponent } from 'react';
import { Link as RouteLink } from 'react-router-dom';
import { Paper, Typography, Box, styled } from '@mui/material';
import { Extension } from '../../extension-registry-types';
import { createRoute } from '../../utils';
import { ExtensionIcon } from '../../components/extension/extension-icon';
import { Timestamp } from '../../components/timestamp';
import { UserSettingsRoutes } from './user-settings-routes';

const getOpacity = (extension: Extension) => {
    if (extension.deprecated) {
        return 0.5;
    } else if (extension.active === false) {
        return 0.75;
    } else {
        return 1;
    }
};

const noOverflow = {
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    ml: 0.5
};

const Paragraph = styled(Box)({
    display: 'flex',
    justifyContent: 'space-between'
});

export const UserNamespaceExtensionListItem: FunctionComponent<UserNamespaceExtensionListItemProps> = props => {
    const { extension } = props;
    const route = createRoute([UserSettingsRoutes.EXTENSIONS, extension.namespace, extension.name]);
    const inactive = extension.active === false;

    const renderStatus = (): ReactNode => {
        if (extension.reviewStatus === 'under_review') {
            return (
                <Box mt={0.25}>
                    <Typography variant='body2' sx={{ fontWeight: 600 }}>
                        Under review
                    </Typography>
                    <Typography variant='body2' sx={{ color: 'text.secondary' }}>
                        {extension.reviewMessage ??
                            'Your extension is being reviewed. Please contact support for details.'}
                    </Typography>
                </Box>
            );
        }

        if (extension.reviewStatus === 'rejected') {
            return (
                <Box mt={0.25}>
                    <Typography variant='body2' sx={{ fontWeight: 600, color: 'error.main' }}>
                        Rejected
                    </Typography>
                    <Typography variant='body2' sx={{ color: 'text.secondary' }}>
                        {extension.reviewMessage ?? 'Your extension could not be published.'}
                    </Typography>
                </Box>
            );
        }

        if (inactive) {
            return <Box mt={0.25}>Deactivated</Box>;
        }

        if (extension.timestamp) {
            return (
                <Paragraph mt={0.25}>
                    <span>Published:</span>
                    <Timestamp value={extension.timestamp} sx={noOverflow} />
                </Paragraph>
            );
        }

        return null;
    };

    const status = renderStatus();

    return extension ? (
        <RouteLink to={route} style={{ textDecoration: 'none' }}>
            <Paper
                elevation={3}
                title={`${extension.namespace}.${extension.name} ${extension.version} ${inactive ? '(deactivated)' : ''}`}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    p: 1,
                    opacity: getOpacity(extension),
                    filter: extension.deprecated ? 'grayscale(100%)' : null
                }}>
                <ExtensionIcon
                    extension={extension}
                    sx={{
                        flex: '0 0 15%',
                        display: 'block',
                        mr: 2,
                        width: '3rem',
                        maxHeight: '4rem'
                    }}
                />
                <Box component='div' sx={{ flex: '1', overflow: 'hidden' }}>
                    <Paragraph>
                        <Typography variant='h6' noWrap sx={{ fontSize: '1.15rem' }}>
                            {extension.displayName ?? extension.name}
                        </Typography>
                    </Paragraph>
                    <Paragraph>
                        <span>Version:</span>
                        <Box component='span' sx={noOverflow}>
                            {extension.version}
                        </Box>
                    </Paragraph>
                    {status}
                </Box>
            </Paper>
        </RouteLink>
    ) : null;
};

export interface UserNamespaceExtensionListItemProps {
    extension: Extension;
    canDelete?: boolean;
}
