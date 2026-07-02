/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext } from 'react';
import { Box } from '@mui/material';
import { MainContext } from '../../context';
import { useSearch } from '../../hooks/use-search';
import { HeroSearch } from './hero-search';
import { BrowseCategories } from './browse-categories';
import { CuratedSections } from './curated-sections';
import { GetInvolved } from './get-involved';
import { DEFAULT_CURATED_SECTIONS, useHomeData } from './use-home-data';

/** Landing page: hero search, category browser, curated extension rows and get-involved cards. */
export const HomePage: FunctionComponent = () => {
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
