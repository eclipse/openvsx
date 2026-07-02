/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box, ButtonBase, SvgIconProps, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { cardHoverLift, cardSurface, focusOutline } from './page-primitives';

const Root = styled(ButtonBase)(({ theme }) => ({
    ...cardSurface(theme),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    textAlign: 'left',
    overflow: 'hidden',
    padding: '1.125rem',
    color: theme.palette.text.primary,
    width: '100%',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    ...cardHoverLift(theme),
    ...focusOutline(theme),
    '& .MuiTouchRipple-root': { color: theme.palette.secondary.main }
}));

export interface CategoryCardProps {
    label: string;
    icon: FunctionComponent<SvgIconProps>;
    onClick: () => void;
}

export const CategoryCard: FunctionComponent<CategoryCardProps> = ({ label, icon: Icon, onClick }) => (
    <Root onClick={onClick}>
        <Box
            sx={{
                width: '2.375rem',
                height: '2.375rem',
                borderRadius: '10px',
                bgcolor: 'accentSoft',
                color: 'secondary.main',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                mb: '0.875rem',
                flexShrink: 0
            }}>
            <Icon sx={{ fontSize: '1.25rem' }} />
        </Box>
        <Typography sx={{ fontSize: '0.9375rem', fontWeight: 600, mb: '0.1875rem' }}>{label}</Typography>
    </Root>
);
