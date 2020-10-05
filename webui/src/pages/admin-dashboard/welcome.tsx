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
import { Typography, Grid, Paper, makeStyles } from '@material-ui/core';
import { Link } from 'react-router-dom';
import { AdminDashboardRoutes } from './admin-dashboard';

const useStyles = makeStyles((theme) => ({
    containerRoot: {
        height: '100%'
    },
    link: {
        color: theme.palette.info.main
    },
    paper: {
        padding: theme.spacing(4)
    },
    title: {
        marginBottom: theme.spacing(2)
    }
}));

export const Welcome: FunctionComponent = props => {
    const classes = useStyles();
    return <>
        <Grid container direction='column' spacing={2} classes={{ root: classes.containerRoot }}>
            <Grid style={{ flex: 1 }} item container>
                <Grid item container direction='column' alignItems='center' justify='flex-end'>
                    <Paper className={classes.paper} variant='outlined' >
                        <Typography className={classes.title} align='center' variant='h5'>Welcome to the Admin Dashboard!</Typography>
                        <Typography align='center'>You can switch pages on the sidepanel menu on the left side.</Typography>
                        <Typography align='center'>
                            Choose between administration for <Link className={classes.link} to={AdminDashboardRoutes.NAMESPACE_ADMIN}>Namespaces</Link> and <Link className={classes.link} to={AdminDashboardRoutes.EXTENSION_ADMIN}>Extensions</Link>.
                        </Typography>
                    </Paper>
                </Grid>
            </Grid>
            <Grid style={{ flex: 4 }} item></Grid>
        </Grid>
    </>;
};