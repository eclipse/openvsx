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
import { ButtonBase, SvgIconProps } from '@mui/material';
import { styled } from '@mui/material/styles';
import { focusOutline } from './page-primitives';

const Root = styled(ButtonBase, {
    shouldForwardProp: prop => prop !== 'isSelected'
})<{ isSelected: boolean }>(({ theme, isSelected }) => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-start',
    gap: '0.5rem',
    width: '100%',
    textAlign: 'left',
    padding: '0.4375rem 0.625rem',
    borderRadius: theme.shape.borderRadius,
    overflow: 'hidden',
    fontSize: '0.8125rem',
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
          },
    ...focusOutline(theme)
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
    <Root isSelected={isSelected} aria-current={isSelected ? 'true' : undefined} onClick={onClick}>
        <Icon sx={{ fontSize: '0.9375rem', opacity: 0.75, flexShrink: 0 }} />
        {label}
    </Root>
);
