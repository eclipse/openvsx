/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import clsx from 'clsx';
import {
    Paper, Card, Grid, Button, Divider, CardActions, CardContent, Hidden, Collapse
} from '@material-ui/core';
import makeStyles from '@material-ui/core/styles/makeStyles';

const useStyles = makeStyles((theme) => ({
    root: {
        display: 'block',
        width: '100%',
        marginLeft: 'auto',
        marginRight: 'auto',
    },
    cardContent: {
        paddingBottom: theme.spacing(1),
        paddingRight: theme.spacing(1),
        paddingLeft: theme.spacing(2),
        paddingTop: theme.spacing(1) + 2
    },
    flex: {
        flexGrow: 1,
    },
    buttons: {
        whiteSpace: 'nowrap',
        alignSelf: 'flex-end',
        paddingLeft: '90px !important',
    },
    label: {
        alignSelf: 'center',
    },
    lightTheme: {
        color: '#000',
        '& a': {
            color: '#000',
            fontWeight: 'bold'
        }
    },
    darkTheme: {
        color: '#fff',
        '& a': {
            color: '#fff',
            fontWeight: 'bold'
        }
    },
    infoLight: {
        backgroundColor: theme.palette.info.light
    },
    infoDark: {
        backgroundColor: theme.palette.info.dark,
    },
    warningLight: {
        backgroundColor: theme.palette.warning.light
    },
    warningDark: {
        backgroundColor: theme.palette.warning.dark,
    }
}));

export const Banner: FunctionComponent<BannerProps> = props => {
    const classes = useStyles();

    const renderButtons = <>
        <span className={classes.flex} />

        <Grid item className={classes.buttons}>
            <Button
                variant='outlined'
                onClick={props.dismissButtonOnClick}
            >
                {props.dismissButtonLabel ?? 'Close'}
            </Button>
        </Grid>
    </>;

    let cardClasses = '';
    if (props.color === 'info') {
        cardClasses = props.theme === 'dark'
            ? ` ${classes.darkTheme} ${classes.infoDark}`
            : ` ${classes.lightTheme} ${classes.infoLight}`;
    } else if (props.color === 'warning') {
        cardClasses = props.theme === 'dark'
            ? ` ${classes.darkTheme} ${classes.warningDark}`
            : ` ${classes.lightTheme} ${classes.warningLight}`;
    }
    return <>
        <Collapse in={props.open}>
            <Paper elevation={0} className={classes.root}>
                <Card elevation={0} className={cardClasses}>
                    <CardContent
                        className={clsx(
                            classes.cardContent
                        )}
                    >
                        <Grid
                            container
                            wrap='nowrap'
                            spacing={2}
                            direction='row'
                            justify='flex-start'
                            alignItems='flex-start'
                        >
                            <Grid item className={classes.label}>
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
