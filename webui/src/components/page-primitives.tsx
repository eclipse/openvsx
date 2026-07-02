/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { Box, Typography } from '@mui/material';
import { alpha, styled, Theme } from '@mui/material/styles';

/** Max width of the centered content column shared by every page section. */
export const CONTENT_MAX_WIDTH = 1320;

/**
 * Horizontally-centered content column with responsive gutters. The single
 * source of truth for page width so sections stay aligned across the app.
 */
export const Section = styled(Box)(({ theme }) => ({
    maxWidth: CONTENT_MAX_WIDTH,
    marginInline: 'auto',
    paddingInline: '1.75rem',
    [theme.breakpoints.down('sm')]: {
        paddingInline: '1rem'
    }
}));

/** Small uppercase label used to head sections, columns and sidebars. */
export const Eyebrow = styled(Typography)(({ theme }) => ({
    fontSize: '0.75rem',
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.08em',
    color: theme.palette.text.disabled
})) as typeof Typography;

/**
 * Elevated surface shared by cards (extensions, categories, panels): paper
 * background, hairline border and the card corner radius from the theme.
 */
export const cardSurface = (theme: Theme) => ({
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadiusCard
});

/** Accent focus ring shared by the search fields and card links. */
export const focusRing = (theme: Theme, extraShadow?: string) => ({
    borderColor: theme.palette.secondary.main,
    boxShadow: `0 0 0 3px ${alpha(theme.palette.secondary.main, 0.16)}${extraShadow ? `, ${extraShadow}` : ''}`
});

/** Hover treatment for chips and pills: accent border and text color. Suppressed on touch devices. */
export const accentHover = (theme: Theme) => ({
    '@media (hover: hover)': {
        '&:hover': {
            borderColor: theme.palette.secondary.main,
            color: theme.palette.secondary.light
        }
    }
});

/** Hover treatment for interactive cards: accent border, shadow and lift. Suppressed on touch devices. */
export const cardHoverLift = (theme: Theme) => ({
    '@media (hover: hover)': {
        '&:hover': {
            borderColor: theme.palette.secondary.main,
            boxShadow: 'var(--shadow)',
            transform: 'translateY(-2px)'
        }
    }
});
