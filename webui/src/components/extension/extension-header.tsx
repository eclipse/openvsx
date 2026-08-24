/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, ReactNode } from 'react';
import { Box, Button, Stack, Typography } from '@mui/material';
import { Link as RouteLink } from 'react-router';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { Extension } from '../../extension-registry-types';
import { ExtensionIcon } from './extension-icon';
import { ExtensionDetailRoutes } from '../../pages/extension-detail/extension-detail-routes';
import { createRoute } from '../../utils';
import { MONO_FONT } from '../../default/theme';

export const ExtensionHeader: FunctionComponent<ExtensionHeaderProps> = ({ extension, actions }) => {
    const publicRoute = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '1.25rem',
                flexWrap: 'wrap',
                pb: '1.5rem',
                mb: '1.75rem',
                borderBottom: '1px solid',
                borderColor: 'divider'
            }}>
            <Box
                sx={{
                    flexShrink: 0,
                    width: '4rem',
                    height: '4rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}>
                <ExtensionIcon extension={extension} sx={{ width: '4rem', maxHeight: '4rem', objectFit: 'contain' }} />
            </Box>
            <Box sx={{ flex: 1, minWidth: '12rem' }}>
                <Typography
                    component='h1'
                    noWrap
                    sx={{ fontSize: '1.625rem', fontWeight: 800, letterSpacing: '-0.02em', mb: '0.375rem' }}>
                    {extension.displayName ?? extension.name}
                </Typography>
                <Typography
                    component='code'
                    sx={{ fontFamily: MONO_FONT, fontSize: '0.8125rem', color: 'text.disabled' }}>
                    {extension.namespace}.{extension.name}
                </Typography>
                {extension.description ? (
                    <Typography
                        sx={{
                            fontSize: '0.84375rem',
                            color: 'text.secondary',
                            lineHeight: 1.5,
                            mt: '0.5rem',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden'
                        }}>
                        {extension.description}
                    </Typography>
                ) : null}
            </Box>
            <Stack direction='row' spacing={2} sx={{ flexShrink: 0 }}>
                {actions}
                {/* A deactivated or removed extension has no public page worth linking to. */}
                {extension.active !== false && !extension.removed ? (
                    <Button
                        variant='outlined'
                        component={RouteLink}
                        to={publicRoute}
                        target='_blank'
                        rel='noopener'
                        startIcon={<OpenInNewIcon />}>
                        View public page
                    </Button>
                ) : null}
            </Stack>
        </Box>
    );
};

export interface ExtensionHeaderProps {
    extension: Extension;
    actions?: ReactNode;
}
