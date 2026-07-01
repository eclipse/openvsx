/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box, ButtonBase, SvgIconProps, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { cardHoverLift, cardSurface } from './layout';

const Root = styled(ButtonBase)(({ theme }) => ({
    ...cardSurface(theme),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    textAlign: 'left',
    overflow: 'hidden',
    padding: '18px',
    color: theme.palette.text.primary,
    width: '100%',
    transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s',
    '&:hover': cardHoverLift(theme)
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
                width: '38px',
                height: '38px',
                borderRadius: '10px',
                bgcolor: 'surface3',
                color: 'text.secondary',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                mb: '14px',
                flexShrink: 0
            }}>
            <Icon sx={{ fontSize: '20px' }} />
        </Box>
        <Typography sx={{ fontSize: '15px', fontWeight: 600, mb: '3px' }}>{label}</Typography>
    </Root>
);
