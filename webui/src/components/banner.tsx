/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, PropsWithChildren } from 'react';
import { Paper, Grid, Button, Collapse } from '@mui/material';

export const Banner: FunctionComponent<PropsWithChildren<BannerProps>> = props => {
    const cardColor = props.theme === 'dark' ? '#fff' : '#000';
    const cardBackground = `${props.color}.${props.theme}`;
    return <>
        <Collapse in={props.open}>
            <Paper
                elevation={0}
                sx={{
                    display: 'block',
                    width: '100%',
                    mx: 'auto',
                    pb: 1, pr: 1, pl: 2, pt: 1.25,
                    color: cardColor,
                    bgcolor: cardBackground,
                    '& a': {
                        color: cardColor,
                        fontWeight: 'bold'
                    }
                }}
            >
                <Grid
                    container
                    spacing={2}
                >
                    <Grid item xs={12} sm sx={{ alignSelf: 'center' }}>
                        {props.children}
                    </Grid>
                    {
                        props.showDismissButton &&
                            <Grid item xs={12} sm='auto' sx={{ whiteSpace: 'nowrap', alignSelf: 'flex-end', paddingLeft: '90px !important' }}>
                                <Button
                                    sx={{ float: 'right' }}
                                    variant='outlined'
                                    onClick={props.dismissButtonOnClick}
                                >
                                    {props.dismissButtonLabel ?? 'Close'}
                                </Button>
                            </Grid>
                    }
                </Grid>
            </Paper>
        </Collapse>
    </>;
};

interface BannerProps {
    open: boolean;
    showDismissButton?: boolean;
    dismissButtonLabel?: string;
    dismissButtonOnClick?: () => void;
    color?: 'info' | 'warning';
    theme?: 'light' | 'dark';
}
