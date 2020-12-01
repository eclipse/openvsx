/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useState } from 'react';
import { Box, Container, makeStyles, CssBaseline, Typography, IconButton } from '@material-ui/core';
import { createRoute } from '../../utils';
import { Sidepanel } from '../../components/sidepanel/sidepanel';
import { NavigationItem } from '../../components/sidepanel/navigation-item';
import AssignmentIndIcon from '@material-ui/icons/AssignmentInd';
import ExtensionSharpIcon from '@material-ui/icons/ExtensionSharp';
import { Route, Switch, useHistory, useLocation } from 'react-router-dom';
import { NamespaceAdmin } from './namespace-admin';
import { ExtensionAdmin } from './extension-admin';
import { MainContext } from '../../context';
import HighlightOffIcon from '@material-ui/icons/HighlightOff';
import { Welcome } from './welcome';
import { PublisherAdmin } from './publisher-admin';
import PersonIcon from '@material-ui/icons/Person';

export namespace AdminDashboardRoutes {
    export const ROOT = 'admin-dashboard';
    export const MAIN = createRoute([ROOT]);
    export const NAMESPACE_ADMIN = createRoute([MAIN, 'namespaces']);
    export const EXTENSION_ADMIN = createRoute([MAIN, 'extensions']);
    export const PUBLISHER_ADMIN = createRoute([MAIN, 'publisher']);
}

const useStyles = makeStyles((theme) => ({
    container: {
        paddingTop: theme.spacing(8),
        height: '100%'
    },
    message: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%'
    }
}));

export const AdminDashboard: FunctionComponent<AdminDashboard.Props> = props => {
    const classes = useStyles();

    const { user } = useContext(MainContext);

    const history = useHistory();
    const toMainPage = () => history.push('/');

    const [currentPage, setCurrentPage] = useState<string | undefined>(useLocation().pathname);
    const handleOpenRoute = (route: string) => setCurrentPage(route);

    return <>
        <CssBaseline />
        <Box display='flex' height='100vh'>
            {
                user?.role === 'admin' ?
                <>
                    <Sidepanel>
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.NAMESPACE_ADMIN} label='Namespaces' icon={<AssignmentIndIcon />} route={AdminDashboardRoutes.NAMESPACE_ADMIN}></NavigationItem>
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.EXTENSION_ADMIN} label='Extensions' icon={<ExtensionSharpIcon />} route={AdminDashboardRoutes.EXTENSION_ADMIN}></NavigationItem>
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.PUBLISHER_ADMIN} label='Publishers' icon={<PersonIcon />} route={AdminDashboardRoutes.PUBLISHER_ADMIN}></NavigationItem>
                    </Sidepanel>
                    <Box overflow='auto' flex={1} >
                        <Container className={classes.container} maxWidth='lg'>
                            <Switch>
                                <Route path={AdminDashboardRoutes.NAMESPACE_ADMIN} component={NamespaceAdmin} />
                                <Route path={AdminDashboardRoutes.EXTENSION_ADMIN} component={ExtensionAdmin} />
                                <Route path={AdminDashboardRoutes.PUBLISHER_ADMIN} component={PublisherAdmin} />
                                <Route path='*' component={Welcome} />
                            </Switch>
                        </Container>
                    </Box>
                </>
                : user ?
                <Box className={classes.message}><Typography variant='h6'>You are not authorized as administrator.</Typography></Box>
                : !props.userLoading ?
                <Box className={classes.message}><Typography variant='h6'>You are not logged in.</Typography></Box>
                : null
            }
            <Box position='absolute' top='5px' right='5px'>
                <IconButton onClick={toMainPage}>
                    <HighlightOffIcon></HighlightOffIcon>
                </IconButton>
            </Box>
        </Box>
    </>;
};

export namespace AdminDashboard {
    export interface Props {
        userLoading: boolean;
    }
}
