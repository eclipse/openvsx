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
    linkContainer: {
        marginTop: theme.spacing(2)
    },
    linkItemContainer: {
        marginBottom: theme.spacing(2)
    },
    link: {
        color: theme.palette.secondary.main,
        textDecoration: 'none',
        '&:hover': {
            textDecoration: 'underline'
        }
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
            <Grid item container direction='column' alignItems='center' justify='flex-end'>
                <Paper className={classes.paper} variant='outlined' >
                    <Typography className={classes.title} align='center' variant='h5'>Welcome to the Admin Dashboard!</Typography>
                    <Typography align='center'>You can switch pages in the sidepanel menu on the left side.</Typography>
                    <Typography align='center'>
                        Choose between administration for
                    </Typography>
                    <Grid container justify='center' alignItems='center' className={classes.linkContainer}>
                        <WelcomeLinkItem route={AdminDashboardRoutes.NAMESPACE_ADMIN} label='Namespaces' description='Manage user roles, create new namespaces' />
                        <WelcomeLinkItem route={AdminDashboardRoutes.EXTENSION_ADMIN} label='Extensions' description='Search for extensions and remove certain versions' />
                        <WelcomeLinkItem route={AdminDashboardRoutes.PUBLISHER_ADMIN} label='Publishers' description='Search for publishers and revoke their contributions' />
                    </Grid>
                </Paper>
            </Grid>
        </Grid>
    </>;
};

const WelcomeLinkItem: FunctionComponent<{ route: string, label: string, description: string }> = props => {
    const classes = useStyles();
    return <>
        <Grid container item xs={8} className={classes.linkItemContainer}>
            <Grid container alignItems='center' item xs={12} md={4}>
                <Typography>
                    <Link className={classes.link} to={props.route}>{props.label}</Link>
                </Typography>
            </Grid>
            <Grid item xs={12} md={8}>
                <Typography variant='body1' style={{ lineHeight: 1.5 }}>{props.description}</Typography>
            </Grid>
        </Grid>
    </>;
};