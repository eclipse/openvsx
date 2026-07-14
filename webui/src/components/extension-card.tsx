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

import { forwardRef, FunctionComponent, memo } from 'react';
import { Link as RouteLink } from 'react-router-dom';
import { Paper, Typography, Box, Fade, Skeleton } from '@mui/material';
import { CSSObject, styled, Theme } from '@mui/material/styles';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import { ExtensionDetailRoutes } from '../pages/extension-detail/extension-detail-routes';
import { SearchEntry } from '../extension-registry-types';
import { ExtensionIcon } from './extension/extension-icon';
import { ExtensionRatingStars } from '../pages/extension-detail/extension-rating-stars';
import { createRoute, formatCompactNumber } from '../utils';
import { MONO_FONT } from '../default/theme';
import { GridItemProps } from '../hooks/use-grid-cursor';
import { cardHoverLift, cardSurface, focusRing } from './page-primitives';

// Shared surface + footprint so the card and its skeleton occupy identical space.
const cardLayout = (theme: Theme): CSSObject => ({
    ...cardSurface(theme),
    padding: '1.375rem 1rem',
    [theme.breakpoints.down('sm')]: { padding: '0.875rem 0.625rem' },
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '100%',
    minHeight: '12.875rem'
});

const CardRoot = styled(Paper)(({ theme }) => ({
    ...cardLayout(theme),
    textAlign: 'center',
    cursor: 'pointer',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    ...cardHoverLift(theme),
    // Keyboard focus ring mirrors the search field's :focus-within style.
    // Ring when the card link is keyboard-focused, or when it is the grid
    // cursor and the cursor is visible (see useGridCursor).
    'a:focus-visible &, [data-cursor-visible] a[data-active] &': focusRing(theme)
}));

const SkeletonRoot = styled(Paper)(({ theme }) => cardLayout(theme));

// Only the unknown parts are skeletons; the stars' empty state looks the same loaded or not.
const SkeletonContent: FunctionComponent = () => (
    <>
        <Skeleton variant='rounded' width={54} height={54} sx={{ flexShrink: 0, mb: '0.875rem' }} />
        <Box sx={{ width: '100%', height: { xs: '2.125rem', sm: '2.375rem' }, overflow: 'hidden' }}>
            <Skeleton variant='text' sx={{ fontSize: { xs: '0.8125rem', sm: '0.875rem' } }} />
            <Skeleton variant='text' width='60%' sx={{ fontSize: { xs: '0.8125rem', sm: '0.875rem' }, mx: 'auto' }} />
        </Box>
        <Box sx={{ width: '100%', mt: '0.875rem', display: 'flex', justifyContent: 'space-between', gap: 1 }}>
            <Skeleton variant='text' width='45%' sx={{ fontSize: '0.75rem' }} />
            <Skeleton variant='text' width='25%' sx={{ fontSize: '0.6875rem' }} />
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
                fontSize: { xs: '0.875rem', sm: '1.25rem' }
            }}>
            <ExtensionRatingStars number={0} fontSize='inherit' />
        </Box>
    </>
);

/** Loading placeholder matching {@link ExtensionCard}'s footprint. */
export const ExtensionCardSkeleton: FunctionComponent = () => (
    <SkeletonRoot elevation={0}>
        <SkeletonContent />
    </SkeletonRoot>
);

// The grid cursor props are optional: cards also render outside a cursor grid
// (curated sections, namespace detail).
export interface ExtensionCardProps extends Partial<Omit<GridItemProps, 'ref'>> {
    /**
     * The extension, or `undefined` for a loading skeleton. Keep a stable key
     * across the swap so the fade plays once instead of restarting.
     */
    extension?: SearchEntry;
    /** Delay before the card fades in, so grids can stagger their cards. */
    fadeDelayMs?: number;
    /** When false, the card shows immediately without its entrance fade (e.g. restored from cache on back-nav). */
    appear?: boolean;
}

export const ExtensionCard = memo(
    forwardRef<HTMLAnchorElement, ExtensionCardProps>(function ExtensionCard(
        { extension, fadeDelayMs = 0, appear = true, ...linkProps },
        ref
    ) {
        const title = extension?.displayName ?? extension?.name;
        const downloadCount = extension ? formatCompactNumber(extension.downloadCount ?? 0) : undefined;

        // One Fade over both states so it runs once and carries through the skeleton → card swap.
        return (
            <Fade in appear={appear} timeout={{ enter: fadeDelayMs }}>
                <Box title={title} sx={{ height: '100%' }}>
                    {extension ? (
                        <RouteLink
                            ref={ref}
                            {...linkProps}
                            to={createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name])}
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
                                    <ExtensionIcon
                                        extension={extension}
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
                                        sx={{
                                            fontSize: '0.75rem',
                                            color: 'text.disabled',
                                            minWidth: 0,
                                            textAlign: 'left'
                                        }}>
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
                                        <ExtensionRatingStars
                                            number={extension.averageRating ?? 0}
                                            fontSize='inherit'
                                        />
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
                    ) : (
                        <SkeletonRoot elevation={0}>
                            <SkeletonContent />
                        </SkeletonRoot>
                    )}
                </Box>
            </Fade>
        );
    })
);
