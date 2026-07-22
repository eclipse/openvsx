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
import { IconButton, IconButtonProps } from '@mui/material';
import { styled } from '@mui/material/styles';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';

// Circular twin of the outlined-error Button (see MuiButton.outlinedError in the theme).
const Root = styled(IconButton)(({ theme }) => ({
    width: '1.875rem',
    height: '1.875rem',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    color: theme.palette.error.main,
    // Background only: transitioning color/border makes the disabled→enabled swap look like a fade.
    transition: 'background-color 0.15s',
    '&:hover': {
        backgroundColor: theme.palette.error.main,
        borderColor: theme.palette.error.main,
        color: theme.palette.common.white
    },
    '&.Mui-disabled': {
        opacity: 0.45,
        color: theme.palette.error.main
    }
}));

/** Standard icon-only delete button: danger-zone styling in a circle with a small bin. */
export const DeleteIconButton: FunctionComponent<IconButtonProps> = props => (
    <Root {...props}>
        <DeleteOutlineIcon sx={{ fontSize: '0.9375rem' }} />
    </Root>
);
