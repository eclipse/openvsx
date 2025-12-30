/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { CssBaseline } from '@mui/material';
import { Route, Routes } from 'react-router-dom';
import { AdminDashboard, AdminDashboardRoutes } from './pages/admin-dashboard/admin-dashboard';
import { ErrorDialog } from './components/error-dialog';
import { MainContext } from './context';
import { PageSettings } from './page-settings';

import '../src/main.css';
import { OtherPages } from './other-pages';
import { useGetLoginProvidersQuery, useGetUserAuthErrorQuery } from './store/api';

export const Main: FunctionComponent<MainProps> = props => {
    const { data: loginProviders } = useGetLoginProvidersQuery(undefined, { skip: props.loginProviders != null });
    useGetUserAuthErrorQuery(undefined, { skip: !new URLSearchParams(window.location.search).has('auth-error') });

    const mainContext: MainContext = {
        handleError: () => {},
        pageSettings: props.pageSettings,
        loginProviders: props.loginProviders ?? loginProviders
    };

    const { mainHeadTags: MainHeadTagsComponent } = props.pageSettings.elements;
    return <>
        <CssBaseline />
        <MainContext.Provider value={mainContext}>
            {MainHeadTagsComponent ? <MainHeadTagsComponent pageSettings={props.pageSettings} /> : null}
            <Routes>
                <Route path={AdminDashboardRoutes.MAIN + '/*'} element={<AdminDashboard />} />
                <Route path='*' element={<OtherPages />} />
            </Routes>
            <ErrorDialog />
        </MainContext.Provider>
    </>;
};

export interface MainProps {
    pageSettings: PageSettings;
    loginProviders?: Record<string, string>;
}