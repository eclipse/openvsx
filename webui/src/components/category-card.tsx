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

import { FunctionComponent } from 'react';
import { Box, ButtonBase, SvgIconProps, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { cardHoverLift, cardSurface, focusOutline } from './page-primitives';

const Root = styled(ButtonBase)(({ theme }) => {
    const hoverLift = cardHoverLift(theme);
    return {
        ...cardSurface(theme),
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'flex-start',
        textAlign: 'left',
        overflow: 'hidden',
        gap: '0.625rem',
        padding: '0.625rem 0.75rem',
        color: theme.palette.text.primary,
        width: '100%',
        transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
        ...hoverLift,
        ...focusOutline(theme),
        '& .MuiTouchRipple-root': { color: theme.palette.secondary.main },
        '& .MuiTypography-root': {
            display: '-webkit-box',
            WebkitBoxOrient: 'vertical',
            WebkitLineClamp: 1,
            overflow: 'hidden'
        },
        '@media (hover: hover)': {
            ...hoverLift['@media (hover: hover)'],
            '&:hover .MuiTypography-root': {
                WebkitLineClamp: 'unset',
                overflow: 'visible'
            }
        }
    };
});

export interface CategoryCardProps {
    label: string;
    icon: FunctionComponent<SvgIconProps>;
    onClick: () => void;
}

export const CategoryCard: FunctionComponent<CategoryCardProps> = ({ label, icon: Icon, onClick }) => (
    <Root onClick={onClick}>
        <Box
            sx={{
                width: '1.875rem',
                height: '1.875rem',
                borderRadius: '8px',
                bgcolor: 'accentSoft',
                color: 'secondary.main',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
            }}>
            <Icon sx={{ fontSize: '1.0625rem' }} />
        </Box>
        <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, pt: '0.28125rem' }}>{label}</Typography>
    </Root>
);
