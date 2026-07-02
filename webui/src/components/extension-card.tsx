/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { memo, useContext, useEffect, useState } from 'react';
import { Link as RouteLink } from 'react-router-dom';
import { Paper, Typography, Box, Fade } from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import { MainContext } from '../context';
import { ExtensionDetailRoutes } from '../pages/extension-detail/extension-detail-routes';
import { SearchEntry } from '../extension-registry-types';
import { ExtensionRatingStars } from '../pages/extension-detail/extension-rating-stars';
import { createRoute, formatCompactNumber } from '../utils';
import { MONO_FONT } from '../default/theme';
import { cardHoverLift, cardSurface } from './layout';

const CardRoot = styled(Paper)(({ theme }) => ({
    ...cardSurface(theme),
    padding: '1.375rem 1rem',
    [theme.breakpoints.down('sm')]: { padding: '0.875rem 0.625rem' },
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    height: '100%',
    minHeight: '12.875rem',
    cursor: 'pointer',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    ...cardHoverLift(theme),
    // Keyboard focus ring mirrors the search field's :focus-within style.
    'a:focus-visible &': {
        borderColor: theme.palette.secondary.main,
        boxShadow: `0 0 0 3px ${alpha(theme.palette.secondary.main, 0.16)}`
    }
}));

export interface ExtensionCardProps {
    extension: SearchEntry;
    idx: number;
    filterSize: number;
}

export const ExtensionCard = memo(function ExtensionCard({ extension, idx, filterSize }: ExtensionCardProps) {
    const context = useContext(MainContext);
    const [icon, setIcon] = useState<string>();

    useEffect(() => {
        const abortController = new AbortController();
        let objectUrl: string | undefined;
        context.service
            .getExtensionIcon(abortController, extension)
            .then(url => {
                objectUrl = url;
                setIcon(url);
            })
            .catch(err => {
                if (!abortController.signal.aborted) context.handleError(err);
            });
        return () => {
            abortController.abort();
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [extension.namespace, extension.name, extension.version]);

    const route = createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name]);
    const downloadCount = formatCompactNumber(extension.downloadCount ?? 0);
    const title = extension.displayName ?? extension.name;

    return (
        <Fade in timeout={{ enter: ((filterSize + idx) % filterSize) * 200 }}>
            <Box title={title} sx={{ height: '100%' }}>
                <RouteLink
                    to={route}
                    data-ext-card
                    aria-label={title}
                    style={{ textDecoration: 'none', height: '100%', display: 'block', outline: 'none' }}>
                    <CardRoot
                        elevation={0}
                        sx={extension.deprecated ? { opacity: 0.5, filter: 'grayscale(100%)' } : undefined}>
                        <Box
                            display='flex'
                            justifyContent='center'
                            alignItems='center'
                            flexShrink={0}
                            sx={{
                                width: 54,
                                height: 54,
                                mb: '0.875rem'
                            }}>
                            <Box
                                component='img'
                                src={icon ?? context.pageSettings.urls.extensionDefaultIcon}
                                alt={title}
                                sx={{ width: 54, maxHeight: 54, objectFit: 'contain' }}
                            />
                        </Box>
                        <Typography
                            sx={{
                                fontSize: { xs: '0.8125rem', sm: '0.875rem' },
                                fontWeight: 700,
                                lineHeight: 1.3,
                                width: '100%',
                                minHeight: { xs: '2.125rem', sm: '2.375rem' },
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
                                gap: 1
                            }}>
                            <Typography
                                component='div'
                                noWrap
                                sx={{ fontSize: '0.75rem', color: 'text.disabled', minWidth: 0, textAlign: 'left' }}>
                                {extension.namespace}
                            </Typography>
                            <Typography
                                component='div'
                                noWrap
                                sx={{
                                    fontSize: '0.6875rem',
                                    color: 'text.disabled',
                                    flexShrink: 0,
                                    fontFamily: MONO_FONT
                                }}>
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
                                justifyContent: 'space-between',
                                fontSize: '0.75rem'
                            }}>
                            <Box sx={{ display: 'flex', fontSize: { xs: '0.875rem', sm: '1.25rem' } }}>
                                <ExtensionRatingStars number={extension.averageRating ?? 0} fontSize='inherit' />
                            </Box>
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
            </Box>
        </Fade>
    );
});
