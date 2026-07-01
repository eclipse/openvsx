/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { ButtonBase, SvgIconProps } from '@mui/material';
import { styled } from '@mui/material/styles';

const Root = styled(ButtonBase, {
    shouldForwardProp: prop => prop !== 'isSelected'
})<{ isSelected: boolean }>(({ theme, isSelected }) => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-start',
    gap: '8px',
    width: '100%',
    textAlign: 'left',
    padding: '7px 10px',
    borderRadius: theme.shape.borderRadius,
    overflow: 'hidden',
    fontSize: '13.5px',
    fontWeight: isSelected ? 600 : 400,
    color: isSelected ? theme.palette.secondary.light : theme.palette.text.secondary,
    backgroundColor: isSelected ? theme.palette.accentSoft : 'transparent',
    marginBottom: '1px',
    fontFamily: 'inherit',
    transition: 'background 0.14s, color 0.14s',
    '&:hover': isSelected
        ? {}
        : {
              backgroundColor: theme.palette.surface3,
              color: theme.palette.text.primary
          }
}));

export interface CategoryListItemProps {
    label: string;
    icon: FunctionComponent<SvgIconProps>;
    isSelected: boolean;
    onClick: () => void;
}

export const CategoryListItem: FunctionComponent<CategoryListItemProps> = ({
    label,
    icon: Icon,
    isSelected,
    onClick
}) => (
    <Root isSelected={isSelected} onClick={onClick}>
        <Icon sx={{ fontSize: '15px', opacity: 0.75, flexShrink: 0 }} />
        {label}
    </Root>
);
