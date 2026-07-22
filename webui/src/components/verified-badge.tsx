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
import { SxProps, Theme, styled } from '@mui/material/styles';
import CheckIcon from '@mui/icons-material/Check';

const Root = styled('span')(({ theme }) => ({
    flexShrink: 0,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '1em',
    height: '1em',
    borderRadius: '50%',
    backgroundColor: theme.palette.secondary.main,
    color: theme.palette.secondary.contrastText
}));

/** Accent circle with a check mark; sized by `fontSize` (e.g. `sx={{ fontSize: '1rem' }}`). */
export const VerifiedBadge: FunctionComponent<VerifiedBadgeProps> = ({ title, sx }) => (
    <Root title={title} sx={sx}>
        <CheckIcon sx={{ fontSize: '0.65em' }} />
    </Root>
);

export interface VerifiedBadgeProps {
    title: string;
    sx?: SxProps<Theme>;
}
