/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, MouseEventHandler, PropsWithChildren } from 'react';
import { Box, Button, CircularProgress, SxProps, Theme } from '@mui/material';

export const ButtonWithProgress: FunctionComponent<PropsWithChildren<ButtonWithProgressProps>> = props => {
    return <Box component='div' sx={[{ position: 'relative' }, ...(Array.isArray(props.sx) ? props.sx : [props.sx])]}>
        <Button
            variant='contained'
            color='secondary'
            disabled={props.working || props.error}
            autoFocus={props.autoFocus}
            onClick={props.onClick}
            title={props.title}
            disableTouchRipple={true}
        >
            {props.children}
        </Button>
        {   props.working ?
            <CircularProgress
                size={24}
                sx={{
                    color: 'secondary.main',
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    mt: '-12px',
                    ml: '-12px'
                }}
            />
            : null
        }
    </Box>;
};

export interface ButtonWithProgressProps {
    working: boolean;
    error?: boolean;
    autoFocus?: boolean;
    onClick: MouseEventHandler<HTMLButtonElement>;
    title?: string;
    sx?: SxProps<Theme>;
}