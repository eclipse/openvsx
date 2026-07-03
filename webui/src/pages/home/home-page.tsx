/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Box } from '@mui/material';
import { Navigate, useSearchParams } from 'react-router-dom';
import { MainContext } from '../../context';
import { HomeSettings } from '../../page-settings';
import { ExtensionListRoutes } from '../extension-list/extension-list-routes';
import { addQuery } from '../../utils';
import { HeroSearch } from './hero-search';
import { BrowseCategories } from './browse-categories';
import { CuratedSections } from './curated-sections';
import { GetInvolved } from './get-involved';

/** Landing page. Pre-redesign search URLs lived here (/?search=...), so redirect those to /search. */
export const HomePage: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const [params] = useSearchParams();
    if (['search', 'category', 'sortBy', 'sortOrder'].some(key => params.has(key))) {
        const target = addQuery(ExtensionListRoutes.SEARCH, [
            { key: 'q', value: params.get('search') ?? undefined },
            { key: 'category', value: params.get('category') ?? undefined },
            { key: 'sortBy', value: params.get('sortBy') ?? undefined },
            { key: 'sortOrder', value: params.get('sortOrder') ?? undefined }
        ]);
        return <Navigate to={target} replace />;
    }
    const home = pageSettings.elements.home;
    if (typeof home === 'function') {
        const CustomHome = home;
        return <CustomHome />;
    }
    return <HomeContent home={home} />;
};

/** The built-in home page: hero search, category browser, curated extension rows and get-involved cards. */
const HomeContent: FunctionComponent<{ home?: HomeSettings }> = ({ home }) => {
    const { pageSettings } = useContext(MainContext);
    return (
        <Box component='main' sx={{ animation: 'fadeIn .25s ease' }}>
            <HeroSearch searchHeader={pageSettings.elements.searchHeader} popularSearches={home?.popularSearches} />
            <BrowseCategories />
            <CuratedSections sections={home?.curatedSections} />
            <GetInvolved heading={home?.involvement?.heading} cards={home?.involvement?.cards} />
        </Box>
    );
};
