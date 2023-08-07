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
import { Box, Paper, Card, Grid, Button, Divider, CardActions, CardContent, Hidden, Collapse } from '@mui/material';

export const Banner: FunctionComponent<PropsWithChildren<BannerProps>> = props => {
    const renderButtons = <>
        <Box sx={{ flexGrow: 1 }} component='span'/>
        <Grid item sx={{ whiteSpace: 'nowrap', alignSelf: 'flex-end', paddingLeft: '90px !important' }}>
            <Button
                variant='outlined'
                onClick={props.dismissButtonOnClick}
            >
                {props.dismissButtonLabel ?? 'Close'}
            </Button>
        </Grid>
    </>;

    const cardColor = props.theme === 'dark' ? '#fff' : '#000';
    const cardBackground = `${props.color}.${props.theme}`;
    return <>
        <Collapse in={props.open}>
            <Paper elevation={0} sx={{ display: 'block', width: '100%', mx: 'auto' }}>
                <Card
                    elevation={0}
                    sx={{
                        color: cardColor,
                        bgcolor: cardBackground,
                        '& a': {
                            color: cardColor,
                            fontWeight: 'bold'
                        }
                    }}
                >
                    <CardContent sx={{ pb: 1, pr: 1, pl: 2, pt: 1.25 }}>
                        <Grid
                            container
                            wrap='nowrap'
                            spacing={2}
                            direction='row'
                            justifyContent='flex-start'
                            alignItems='flex-start'
                        >
                            <Grid item sx={{ alignSelf: 'center' }}>
                                {props.children}
                            </Grid>
                            {
                                props.showDismissButton ?
                                    <Hidden smDown>
                                        {renderButtons}
                                    </Hidden> : null
                            }
                        </Grid>
                    </CardContent>
                    {
                        props.showDismissButton ?
                            <>
                                <Hidden mdUp>
                                    <CardActions>
                                        {renderButtons}
                                    </CardActions>
                                </Hidden>

                                <Hidden smDown>
                                    <div />
                                </Hidden>
                            </> : null
                    }
                </Card>

                <Divider />
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
