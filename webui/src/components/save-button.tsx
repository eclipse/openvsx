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
import { Button, ButtonProps } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import SaveIcon from '@mui/icons-material/Save';

/**
 * Save action that confirms success in place: while `saved` is on it turns
 * green with a check instead of showing a separate success banner. The caller
 * owns the timing (flip `saved` back after a short timeout).
 */
export const SaveButton: FunctionComponent<SaveButtonProps> = ({
    saved,
    savedLabel = 'Saved',
    children = 'Save',
    disabled,
    sx,
    ...buttonProps
}) => (
    <Button
        variant='contained'
        disabled={disabled || saved}
        startIcon={saved ? <CheckIcon /> : <SaveIcon />}
        sx={[
            {
                transition: 'background-color 0.5s ease',
                ...(saved && {
                    backgroundColor: 'success.main',
                    '&:hover': { backgroundColor: 'success.dark' },
                    '&.Mui-disabled': { backgroundColor: 'success.main', color: 'white', opacity: 0.9 }
                })
            },
            ...(Array.isArray(sx) ? sx : [sx])
        ]}
        {...buttonProps}>
        {saved ? savedLabel : children}
    </Button>
);

export interface SaveButtonProps extends ButtonProps {
    /** Show the green "saved" confirmation state (also disables the button). */
    saved: boolean;
    savedLabel?: string;
}
