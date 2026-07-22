/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode } from 'react';
import { Link as RouteLink } from 'react-router';
import { Box, Paper, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import { Extension } from '../../extension-registry-types';
import { ExtensionIcon } from './extension-icon';
import { getExtensionStatus } from './extension-status';
import { ExtensionRatingStars } from '../../pages/extension-detail/extension-rating-stars';
import { createRoute, formatCompactNumber } from '../../utils';
import { MONO_FONT } from '../../default/theme';
import { cardHoverLift, cardSurface, focusRing } from '../page-primitives';
import { VerifiedBadge } from '../verified-badge';
import { ExtensionSettingsBackState } from '../../pages/user/extensions/extension-settings';

const CardRoot = styled(Paper)(({ theme }) => ({
    ...cardSurface(theme),
    position: 'relative',
    height: '100%',
    minHeight: '12.875rem',
    padding: '1.375rem 1rem 1rem',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    cursor: 'pointer',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    ...cardHoverLift(theme),
    'a:focus-visible &': focusRing(theme),
    '& .manage-gear': {
        position: 'absolute',
        top: '0.75rem',
        right: '0.75rem',
        display: 'flex',
        color: theme.palette.text.disabled,
        opacity: 0.28,
        transition: 'opacity 0.15s'
    },
    '@media (hover: hover)': {
        '&:hover .manage-gear': {
            opacity: 0.7,
            color: theme.palette.secondary.light
        }
    }
}));

const getOpacity = (extension: Extension) => {
    if (extension.deprecated) {
        return 0.5;
    } else if (extension.active === false) {
        return 0.75;
    } else {
        return 1;
    }
};

/**
 * Card linking to the settings page of one of the user's extensions. Mirrors the
 * public {@link ExtensionCard} look with a gear affordance and publisher row.
 */
export const ManageExtensionCard: FunctionComponent<ManageExtensionCardProps> = ({
    extension,
    routePrefix,
    linkState
}) => {
    const title = extension.displayName ?? extension.name;
    const status = getExtensionStatus(extension);
    const downloadCount = formatCompactNumber(extension.downloadCount ?? 0);
    const route = createRoute([routePrefix, extension.namespace, extension.name]);

    let footerStart: ReactNode;
    if (status) {
        footerStart = (
            <Typography component='span' sx={{ fontSize: '0.75rem', fontWeight: 600, color: status.color }}>
                {status.label}
            </Typography>
        );
    } else {
        footerStart = (
            <Box sx={{ display: 'flex', fontSize: { xs: '0.875rem', sm: '1.25rem' } }}>
                <ExtensionRatingStars number={extension.averageRating ?? 0} fontSize='inherit' />
            </Box>
        );
    }

    return (
        <RouteLink
            to={route}
            state={linkState}
            aria-label={title}
            title={`${extension.namespace}.${extension.name} ${extension.version}${status ? ` (${status.label})` : ''}`}
            style={{ textDecoration: 'none', height: '100%', display: 'block', outline: 'none' }}>
            <CardRoot
                elevation={0}
                sx={{
                    opacity: getOpacity(extension),
                    filter: extension.deprecated ? 'grayscale(100%)' : undefined
                }}>
                <span className='manage-gear'>
                    <SettingsOutlinedIcon sx={{ fontSize: '0.9375rem' }} />
                </span>
                <Box
                    sx={{
                        width: 54,
                        height: 54,
                        mb: '0.875rem',
                        flexShrink: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}>
                    <ExtensionIcon
                        extension={extension}
                        alt={title}
                        sx={{ width: 54, maxHeight: 54, objectFit: 'contain' }}
                    />
                </Box>
                <Typography
                    sx={{
                        fontSize: '0.90625rem',
                        fontWeight: 700,
                        lineHeight: 1.3,
                        width: '100%',
                        minHeight: '2.375rem',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden'
                    }}>
                    {title}
                </Typography>
                <Box
                    sx={{
                        width: '100%',
                        mt: '0.875rem',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '0.5rem',
                        fontSize: '0.75rem',
                        color: 'text.disabled'
                    }}>
                    <Box component='span' sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem', minWidth: 0 }}>
                        <Typography component='span' noWrap sx={{ fontSize: 'inherit', color: 'inherit' }}>
                            {extension.namespace}
                        </Typography>
                        {extension.verified ? (
                            <VerifiedBadge title='Verified publisher' sx={{ fontSize: '0.8125rem' }} />
                        ) : null}
                    </Box>
                    <Typography
                        component='span'
                        noWrap
                        sx={{ flexShrink: 0, fontFamily: MONO_FONT, fontSize: '0.6875rem', color: 'inherit' }}>
                        {extension.version}
                    </Typography>
                </Box>
                <Box
                    sx={{
                        width: '100%',
                        mt: 'auto',
                        pt: '0.6875rem',
                        borderTop: '1px solid',
                        borderColor: 'border2',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between'
                    }}>
                    {footerStart}
                    {downloadCount !== '0' && (
                        <Box
                            component='span'
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.25rem',
                                fontFamily: MONO_FONT,
                                fontSize: '0.6875rem',
                                color: 'text.disabled'
                            }}>
                            <SaveAltIcon sx={{ fontSize: '0.8125rem' }} />
                            {downloadCount}
                        </Box>
                    )}
                </Box>
            </CardRoot>
        </RouteLink>
    );
};

export interface ManageExtensionCardProps {
    extension: Extension;
    /** Base route the card links to, e.g. the user settings or the admin extension route. */
    routePrefix: string;
    /** Router state passed to the extension settings page, e.g. its back-navigation target. */
    linkState?: ExtensionSettingsBackState;
}
