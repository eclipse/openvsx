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

import { forwardRef, FunctionComponent, memo, ReactNode } from 'react';
import { Link as RouteLink } from 'react-router';
import { Paper, Typography, Box, Fade, Skeleton } from '@mui/material';
import { alpha, CSSObject, styled, Theme } from '@mui/material/styles';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import VerifiedIcon from '@mui/icons-material/Verified';
import { ExtensionDetailRoutes } from '../pages/extension-detail/extension-detail-routes';
import { Extension, SearchEntry } from '../extension-registry-types';
import { ExtensionIcon } from './extension/extension-icon';
import { ExtensionRatingStars } from '../pages/extension-detail/extension-rating-stars';
import { createRoute, formatCompactNumber } from '../utils';
import { MONO_FONT } from '../default/theme';
import { GridItemProps } from '../hooks/use-grid-cursor';
import { cardHoverLift, cardSurface, focusRing } from './page-primitives';

// Shared surface + footprint so the card and its skeleton occupy identical space.
// A size container so tight cards can shed the footer's review count.
const cardLayout = (theme: Theme): CSSObject => ({
    ...cardSurface(theme),
    containerType: 'inline-size',
    padding: '1rem 0.875rem',
    [theme.breakpoints.down('sm')]: { padding: '0.75rem 0.625rem' },
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '100%',
    minHeight: '13rem'
});

const CardRoot = styled(Paper)(({ theme }) => ({
    ...cardLayout(theme),
    position: 'relative',
    textAlign: 'center',
    cursor: 'pointer',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    ...cardHoverLift(theme, {
        '&:hover .extension-card-overlay': {
            opacity: 0.7,
            color: theme.palette.secondary.light
        }
    }),
    // Quiet corner affordance (see the `overlay` prop): barely there until the card is hovered.
    '& .extension-card-overlay': {
        position: 'absolute',
        top: '0.75rem',
        right: '0.75rem',
        display: 'flex',
        color: theme.palette.text.disabled,
        opacity: 0.28,
        transition: 'opacity 0.15s, color 0.15s'
    },
    // Keyboard focus ring mirrors the search field's :focus-within style.
    // Ring when the card link is keyboard-focused, or when it is the grid
    // cursor and the cursor is visible (see useGridCursor).
    'a:focus-visible &, [data-cursor-visible] a[data-active] &': focusRing(theme)
}));

const SkeletonRoot = styled(Paper)(({ theme }) => cardLayout(theme));

// Only the unknown parts are skeletons; the stars' empty state looks the same loaded or not.
const SkeletonContent: FunctionComponent = () => (
    <>
        <Skeleton variant='rounded' width={54} height={54} sx={{ flexShrink: 0, mb: '0.75rem' }} />
        <Skeleton variant='text' width='70%' sx={{ fontSize: { xs: '0.8125rem', sm: '0.875rem' } }} />
        <Box sx={{ width: '100%', height: '2.1rem', mt: '0.375rem', overflow: 'hidden' }}>
            <Skeleton variant='text' sx={{ fontSize: '0.75rem' }} />
            <Skeleton variant='text' width='60%' sx={{ fontSize: '0.75rem', mx: 'auto' }} />
        </Box>
        <Box sx={{ width: '100%', mt: '0.75rem', display: 'flex', justifyContent: 'space-between', gap: 1 }}>
            <Skeleton variant='text' width='45%' sx={{ fontSize: '0.75rem' }} />
            <Skeleton variant='text' width='30%' sx={{ fontSize: '0.6875rem' }} />
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
                gap: '0.0625rem',
                fontSize: { xs: '0.8125rem', sm: '0.875rem' }
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
    extension?: Extension | SearchEntry;
    /** Delay before the card fades in, so grids can stagger their cards. */
    fadeDelayMs?: number;
    /** When false, the card shows immediately without its entrance fade (e.g. restored from cache on back-nav). */
    appear?: boolean;
    /** Link target; defaults to the extension's public detail page. */
    to?: string;
    /** Router state carried by the link, e.g. the manage page's back-navigation target. */
    linkState?: unknown;
    /** Corner affordance drawn over the card, e.g. the manage gear. */
    overlay?: ReactNode;
    /** Replaces the rating stars in the footer, e.g. with a publishing-status label. */
    footerStart?: ReactNode;
    /** Dim and desaturate the card the way a deprecated extension is dimmed. */
    dimmed?: boolean;
    /** Tint the card's frame and overlay to flag a state the viewer has to act on. */
    tone?: 'warning';
    /** The icon is still being produced (e.g. a just-published package), so keep its skeleton. */
    iconPending?: boolean;
}

export const ExtensionCard = memo(
    forwardRef<HTMLAnchorElement, ExtensionCardProps>(function ExtensionCard(
        {
            extension,
            fadeDelayMs = 0,
            appear = true,
            to,
            linkState,
            overlay,
            footerStart,
            dimmed,
            tone,
            iconPending,
            ...linkProps
        },
        ref
    ) {
        const title = extension?.displayName ?? extension?.name;
        const downloadCount = extension ? formatCompactNumber(extension.downloadCount ?? 0) : undefined;
        const reviewCount = extension?.reviewCount ?? 0;

        // One Fade over both states so it runs once and carries through the skeleton → card swap.
        return (
            <Fade in appear={appear} timeout={{ enter: fadeDelayMs }}>
                <Box title={title} sx={{ height: '100%' }}>
                    {extension ? (
                        <RouteLink
                            ref={ref}
                            {...linkProps}
                            to={to ?? createRoute([ExtensionDetailRoutes.ROOT, extension.namespace, extension.name])}
                            state={linkState}
                            aria-label={title}
                            style={{ textDecoration: 'none', height: '100%', display: 'block', outline: 'none' }}>
                            <CardRoot
                                elevation={0}
                                sx={[
                                    (extension.deprecated || dimmed) === true && {
                                        opacity: 0.5,
                                        filter: 'grayscale(100%)'
                                    },
                                    // The overlay is a quiet affordance by default; a toned card is
                                    // flagging something, so it carries the tint at full strength.
                                    tone === 'warning' &&
                                        (theme => ({
                                            borderColor: alpha(theme.palette.warningAccent, 0.55),
                                            '& .extension-card-overlay': {
                                                opacity: 1,
                                                color: theme.palette.warningAccent
                                            },
                                            '@media (hover: hover)': {
                                                '&:hover': { borderColor: theme.palette.warningAccent },
                                                '&:hover .extension-card-overlay': {
                                                    color: theme.palette.warningAccent
                                                }
                                            }
                                        }))
                                ]}>
                                {overlay ? <span className='extension-card-overlay'>{overlay}</span> : null}
                                <Box
                                    display='flex'
                                    justifyContent='center'
                                    alignItems='center'
                                    flexShrink={0}
                                    sx={{
                                        width: 54,
                                        height: 54,
                                        mb: '0.75rem'
                                    }}>
                                    <ExtensionIcon
                                        extension={extension}
                                        alt={title}
                                        pending={iconPending}
                                        sx={{ width: 54, maxHeight: 54, objectFit: 'contain' }}
                                    />
                                </Box>
                                <Typography
                                    noWrap
                                    sx={{
                                        fontSize: { xs: '0.8125rem', sm: '0.875rem' },
                                        fontWeight: 600,
                                        lineHeight: 1.3,
                                        width: '100%'
                                    }}>
                                    {title}
                                </Typography>
                                {/* Fixed two-line box so the meta rows align across cards with short descriptions. */}
                                <Typography
                                    sx={{
                                        fontSize: '0.75rem',
                                        color: 'text.secondary',
                                        lineHeight: 1.4,
                                        height: '2.1rem',
                                        mt: '0.375rem',
                                        width: '100%',
                                        display: '-webkit-box',
                                        WebkitLineClamp: 2,
                                        WebkitBoxOrient: 'vertical',
                                        overflow: 'hidden'
                                    }}>
                                    {extension.description}
                                </Typography>
                                <Box
                                    sx={{
                                        width: '100%',
                                        mt: '0.75rem',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        gap: '0.375rem',
                                        overflow: 'hidden',
                                        // Hovering a truncated side unfolds it; its neighbor yields instead.
                                        '& > :first-of-type:hover': { flexShrink: 0, maxWidth: 'none' },
                                        '& > :first-of-type:hover + *': { minWidth: 0, flexShrink: 1 },
                                        '& > :last-of-type:hover': { flexShrink: 0, maxWidth: 'none' },
                                        '&:has(> :last-of-type:hover) > :first-of-type': {
                                            flexShrink: 1,
                                            minWidth: 0
                                        }
                                    }}>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '0.25rem',
                                            minWidth: 0,
                                            // Both sides trim under pressure, but the namespace keeps
                                            // priority: it shrinks at a third of the version's rate.
                                            flexShrink: 1,
                                            maxWidth: '70%',
                                            // When the version unfolds and this side collapses, clip
                                            // the badge instead of letting it paint over the version.
                                            overflow: 'hidden'
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
                                        {extension.verified && (
                                            <Box
                                                component='span'
                                                title='Verified publisher'
                                                role='img'
                                                aria-label='Verified publisher'
                                                sx={{
                                                    display: 'inline-flex',
                                                    fontSize: '0.75rem',
                                                    color: 'secondary.light',
                                                    flexShrink: 0
                                                }}>
                                                <VerifiedIcon fontSize='inherit' />
                                            </Box>
                                        )}
                                    </Box>
                                    <Typography
                                        component='div'
                                        title={extension.version}
                                        sx={{
                                            fontSize: '0.6875rem',
                                            color: 'text.disabled',
                                            // Yields faster than the namespace, down to one glyph
                                            // plus the ellipsis; long calendar versions cap at 10ch.
                                            flexShrink: 3,
                                            minWidth: '2ch',
                                            maxWidth: '10ch',
                                            fontFamily: MONO_FONT,
                                            whiteSpace: 'nowrap',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis'
                                        }}>
                                        {extension.version}
                                    </Typography>
                                </Box>
                                {extension.deprecated && (
                                    <Box
                                        sx={{
                                            width: '100%',
                                            mt: '0.25rem',
                                            textAlign: 'left',
                                            fontSize: '0.6875rem',
                                            fontWeight: 500,
                                            color: 'warningAccent'
                                        }}>
                                        deprecated
                                    </Box>
                                )}
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
                                    {footerStart ?? (
                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '0.0625rem',
                                                fontSize: { xs: '0.8125rem', sm: '0.875rem' }
                                            }}>
                                            <ExtensionRatingStars
                                                number={extension.averageRating ?? 0}
                                                fontSize='inherit'
                                            />
                                            {reviewCount > 0 && (
                                                <Box
                                                    component='span'
                                                    sx={{
                                                        fontSize: '0.6875rem',
                                                        color: 'text.disabled',
                                                        ml: '0.1875rem',
                                                        // Sheds first on tight cards; the query measures
                                                        // the card's content box, i.e. this row's width.
                                                        '@container (max-width: 134px)': { display: 'none' }
                                                    }}>
                                                    ({formatCompactNumber(reviewCount)})
                                                </Box>
                                            )}
                                        </Box>
                                    )}
                                    {downloadCount !== '0' && (
                                        <Box
                                            component='span'
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '0.1875rem',
                                                flexShrink: 0,
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
