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

export namespace AdminDashboardRoutes {
    export const ROOT = 'admin-dashboard';
    export const MAIN = createRoute([ROOT]);
    export const NAMESPACE_ADMIN = createRoute([ROOT, 'namespaces']);
    export const EXTENSION_ADMIN = createRoute([ROOT, 'extensions']);
    export const PUBLISHER_ADMIN = createRoute([ROOT, 'publisher']);
}

export const AdminDashboard: FunctionComponent<AdminDashboardProps> = props => {
    const { user } = useContext(MainContext);

    const navigate = useNavigate();
    const toMainPage = () => navigate('/');

    const [currentPage, setCurrentPage] = useState<string | undefined>(useLocation().pathname);
    const handleOpenRoute = (route: string) => setCurrentPage(route);

    const message = user ? 'You are not authorized as administrator.' : !props.userLoading ? 'You are not logged in.' : null;
    return <>
        <CssBaseline />
        <Box display='flex' height='100vh'>
            {
                user?.role === 'admin' ?
                <>
                    <Sidepanel>
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.NAMESPACE_ADMIN} label='Namespaces' icon={<AssignmentIndIcon />} route={AdminDashboardRoutes.NAMESPACE_ADMIN} />
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.EXTENSION_ADMIN} label='Extensions' icon={<ExtensionSharpIcon />} route={AdminDashboardRoutes.EXTENSION_ADMIN} />
                        <NavigationItem onOpenRoute={handleOpenRoute} active={currentPage === AdminDashboardRoutes.PUBLISHER_ADMIN} label='Publishers' icon={<PersonIcon />} route={AdminDashboardRoutes.PUBLISHER_ADMIN} />
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
                                <Route path='*' element={<Welcome/>} />
                            </Routes>
                        </Container>
                    </Box>
                </>
                : message ?
                    <Box sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        width: '100%'
                    }}>
                        <Typography variant='h6'>{message}</Typography>
                    </Box>
                : null
            }
        </Box>
    </>;
};

export interface AdminDashboardProps {
    userLoading: boolean;
}