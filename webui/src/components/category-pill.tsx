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

import { FunctionComponent, useEffect, useRef } from 'react';
import { ButtonBase, SvgIconProps } from '@mui/material';
import { styled } from '@mui/material/styles';
import { accentHover, focusOutline } from './page-primitives';

const Root = styled(ButtonBase, {
    shouldForwardProp: prop => prop !== 'isSelected'
})<{ isSelected?: boolean }>(({ theme, isSelected }) => ({
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.4375rem',
    flexShrink: 0,
    overflow: 'hidden',
    backgroundColor: isSelected ? theme.palette.accentSoft : theme.palette.surface2,
    border: `1px solid ${isSelected ? theme.palette.secondary.main : theme.palette.divider}`,
    color: isSelected ? theme.palette.secondary.light : theme.palette.text.secondary,
    fontSize: '0.8125rem',
    fontWeight: isSelected ? 600 : 500,
    padding: '0.4375rem 0.8125rem',
    borderRadius: '999px',
    whiteSpace: 'nowrap',
    fontFamily: 'inherit',
    transition: 'border-color 0.14s, color 0.14s',
    ...(isSelected ? {} : accentHover(theme)),
    ...focusOutline(theme)
}));

export interface CategoryPillProps {
    label: string;
    icon: FunctionComponent<SvgIconProps>;
    isSelected?: boolean;
    onClick: () => void;
}

export const CategoryPill: FunctionComponent<CategoryPillProps> = ({ label, icon: Icon, isSelected, onClick }) => {
    const ref = useRef<HTMLButtonElement>(null);

    // Keep the selected pill visible when the row overflows (deep links, home tiles).
    useEffect(() => {
        if (isSelected) {
            ref.current?.scrollIntoView({ block: 'nearest', inline: 'center', behavior: 'smooth' });
        }
    }, [isSelected]);

    return (
        <Root ref={ref} isSelected={isSelected} aria-pressed={!!isSelected} onClick={onClick}>
            <Icon sx={{ fontSize: '1rem', flexShrink: 0, color: 'secondary.main' }} />
            {label}
        </Root>
    );
};
