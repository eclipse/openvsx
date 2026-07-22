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
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { cardSurface, Eyebrow } from './page-primitives';
import { MONO_FONT } from '../default/theme';

/**
 * Bordered card of rows separated by hairlines (details previews, token lists,
 * the extension general grid, …). The card owns the row separators, so rows
 * don't draw their own borders.
 */
// eslint-disable-next-line react-refresh/only-export-components
export const DetailsCard = styled(Box)(({ theme }) => ({
    ...cardSurface(theme),
    overflow: 'hidden',
    '& > *': { borderBottom: `1px solid ${theme.palette.border2}` },
    '& > *:last-child': { borderBottom: 0 }
}));

/** Label/value row inside a {@link DetailsCard}. */
export const DetailRow: FunctionComponent<DetailRowProps> = ({ label, mono, noWrap, empty, children }) => (
    <Box sx={{ display: 'flex', p: '0.875rem 1.125rem' }}>
        <Typography
            component='span'
            sx={{ width: '8.125rem', flexShrink: 0, fontSize: '0.8125rem', color: 'text.disabled' }}>
            {label}
        </Typography>
        <Typography
            component='span'
            noWrap={noWrap}
            sx={{
                fontSize: '0.875rem',
                fontWeight: empty ? 400 : 600,
                color: empty ? 'text.disabled' : undefined,
                minWidth: 0,
                overflowWrap: noWrap ? undefined : 'anywhere',
                fontFamily: mono ? MONO_FONT : undefined
            }}>
            {children}
        </Typography>
    </Box>
);

export interface DetailRowProps {
    label: string;
    mono?: boolean;
    /** Truncate the value on one line instead of wrapping. */
    noWrap?: boolean;
    /** Mutes the value to signal a field that hasn't been filled in yet. */
    empty?: boolean;
    children: ReactNode;
}

/** Slim uppercase heading inside a {@link DetailsCard}, splitting the rows below it into a named group. */
export const DetailsGroupLabel: FunctionComponent<{ children: ReactNode }> = ({ children }) => (
    <Eyebrow sx={{ fontSize: '0.6875rem', fontWeight: 700, color: 'text.secondary', p: '0.4375rem 0.8rem' }}>
        {children}
    </Eyebrow>
);
