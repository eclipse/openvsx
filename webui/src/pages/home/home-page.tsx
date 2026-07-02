/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Box } from '@mui/material';
import { Navigate, useSearchParams } from 'react-router-dom';
import { MainContext } from '../../context';
import { useSearch } from '../../hooks/use-search';
import { ExtensionListRoutes } from '../extension-list/extension-list-routes';
import { addQuery } from '../../utils';
import { HeroSearch } from './hero-search';
import { BrowseCategories } from './browse-categories';
import { CuratedSections } from './curated-sections';
import { GetInvolved } from './get-involved';
import { DEFAULT_CURATED_SECTIONS, useHomeData } from './use-home-data';

/** Landing page. Pre-redesign search URLs lived here (/?search=...), so redirect those to /search. */
export const HomePage: FunctionComponent = () => {
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
    return <HomeContent />;
};

/** Hero search, category browser, curated extension rows and get-involved cards. */
const HomeContent: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const { search } = useSearch();
    const home = pageSettings.elements.home;
    const curatedSections = home?.curatedSections ?? DEFAULT_CURATED_SECTIONS;
    const { categories, rows } = useHomeData(curatedSections);

    return (
        <Box component='main' sx={{ animation: 'fadeIn .25s ease' }}>
            <HeroSearch
                searchHeader={pageSettings.elements.searchHeader}
                popularSearches={home?.popularSearches ?? []}
            />
            <BrowseCategories categories={categories} onSelect={category => search({ query: '', category })} />
            <CuratedSections rows={rows} onSeeAll={() => search({ query: '' })} />
            <GetInvolved involvement={home?.involvement} />
        </Box>
    );
};
