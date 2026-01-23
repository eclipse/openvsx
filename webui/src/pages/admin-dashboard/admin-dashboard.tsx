/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode, useContext, useState } from 'react';
import { Box, Container, CssBaseline, Typography, IconButton } from '@mui/material';
import { createRoute } from '../../utils';
import { Sidepanel } from '../../components/sidepanel/sidepanel';
import { NavigationItem } from '../../components/sidepanel/navigation-item';
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd';
import ExtensionSharpIcon from '@mui/icons-material/ExtensionSharp';
import { Route, Routes, useNavigate, useLocation } from 'react-router-dom';
import { NamespaceAdmin } from './namespace-admin';
import { ExtensionAdmin } from './extension-admin';
import { MainContext } from '../../context';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import { Welcome } from './welcome';
import { PublisherAdmin } from './publisher-admin';
import PersonIcon from '@mui/icons-material/Person';
import PeopleIcon from '@mui/icons-material/People';
import StarIcon from '@mui/icons-material/Star';
import { Tiers } from './tiers/tiers';
import { Customers } from './customers/customers';

export namespace AdminDashboardRoutes {
    export const ROOT = 'admin-dashboard';
    export const MAIN = createRoute([ROOT]);
    export const NAMESPACE_ADMIN = createRoute([ROOT, 'namespaces']);
    export const EXTENSION_ADMIN = createRoute([ROOT, 'extensions']);
    export const PUBLISHER_ADMIN = createRoute([ROOT, 'publisher']);
    export const TIERS = createRoute([ROOT, 'tiers']);
    export const CUSTOMERS = createRoute([ROOT, 'customers']);
}

const Message: FunctionComponent<{message: string}> = ({ message }) => {
    return (<Box sx={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%'
    }}>
        <Typography variant='h6'>{message}</Typography>
    </Box>);
};

export const AdminDashboard: FunctionComponent<AdminDashboardProps> = props => {
    const { user, loginProviders } = useContext(MainContext);

    const navigate = useNavigate();
    const toMainPage = () => navigate('/');

    const [currentPage, setCurrentPage] = useState<string | undefined>(useLocation().pathname);
    const handleOpenRoute = (route: string) => setCurrentPage(route);

    let content: ReactNode = null;
    if (user?.role === 'admin') {
        content = <>
            <Sidepanel>
                <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.NAMESPACE_ADMIN} label='Namespaces' icon={<AssignmentIndIcon />} route={AdminDashboardRoutes.NAMESPACE_ADMIN} />
                <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.EXTENSION_ADMIN} label='Extensions' icon={<ExtensionSharpIcon />} route={AdminDashboardRoutes.EXTENSION_ADMIN} />
                <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.PUBLISHER_ADMIN} label='Publishers' icon={<PersonIcon />} route={AdminDashboardRoutes.PUBLISHER_ADMIN} />
                <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.TIERS} label='Tiers' icon={<StarIcon />} route={AdminDashboardRoutes.TIERS} />
                <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.CUSTOMERS} label='Customers' icon={<PeopleIcon />} route={AdminDashboardRoutes.CUSTOMERS} />
            </Sidepanel>
            <Box overflow='auto' flex={1}>
                <IconButton onClick={toMainPage} sx={{ float: 'right', mt: 1, mr: 1 }}>
                    <HighlightOffIcon/>
                </IconButton>
                <Container sx={{ pt: 8, height: '100%' }} maxWidth='lg'>
                    <Routes>
                        <Route path='/namespaces' element={<NamespaceAdmin/>} />
                        <Route path='/extensions' element={<ExtensionAdmin/>} />
                        <Route path='/publisher' element={<PublisherAdmin/>} />
                        <Route path='/tiers' element={<Tiers/>} />
                        <Route path='/customers' element={<Customers/>} />
                        <Route path='*' element={<Welcome/>} />
                    </Routes>
                </Container>
            </Box>
        </>;
    } else if (user) {
        content = <Message message='You are not authorized as administrator.'/>;
    } else if (!props.userLoading && loginProviders) {
        content = <Message message='You are not logged in.'/>;
    }

    return <>
        <CssBaseline />
        <Box display='flex' height='100vh'>{content}</Box>
    </>;
};

export interface AdminDashboardProps {
    userLoading: boolean;
}