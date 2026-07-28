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

import { FunctionComponent, useContext, useLayoutEffect } from 'react';
import { useSearchParams } from 'react-router';
import { PageContainer } from '../../components/page-container';
import { SectionSeparator, SectionStack } from '../../components/page-primitives';
import { MainContext } from '../../context';
import { HomeSettings } from '../../page-settings';
import { useSearch } from '../../hooks/use-search';
import { HeroSearch } from './hero-search';
import { BrowseCategories } from './browse-categories';
import { CuratedSections } from './curated-sections';
import { GetInvolved } from './get-involved';

/** Landing page. Pre-redesign search URLs lived here (/?search=...), so redirect those to /search. */
export const HomePage: FunctionComponent = () => {
    const { pageSettings } = useContext(MainContext);
    const [params] = useSearchParams();
    const { search } = useSearch();
    // `search` maps `?search=` → `?q=` and carries the other params through; the
    // other keys already match the filter. Redirecting via `search` (not a bare
    // <Navigate>) sets the field; `replace` keeps the legacy URL out of history.
    const isLegacySearchUrl = ['search', 'category', 'sortBy', 'sortOrder'].some(key => params.has(key));
    useLayoutEffect(() => {
        if (isLegacySearchUrl) {
            search({ query: params.get('search') ?? '' }, { replace: true });
        }
    }, [isLegacySearchUrl, params, search]);
    if (isLegacySearchUrl) {
        return null;
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
        <PageContainer fluid component='main' sx={{ animation: 'fadeIn .25s ease' }}>
            <SectionStack>
                <HeroSearch searchHeader={pageSettings.elements.searchHeader} popularSearches={home?.popularSearches} />
                <SectionSeparator />
                <CuratedSections sections={home?.curatedSections} />
                <BrowseCategories />
                <GetInvolved heading={home?.involvement?.heading} cards={home?.involvement?.cards} />
            </SectionStack>
        </PageContainer>
    );
};
