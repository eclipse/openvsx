/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useCallback, useContext, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { SearchContext } from '../context/search/search-context';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { SortBy, SortOrder } from '../extension-registry-types';
import { ExtensionCategory } from '../pages/search/use-categories';
import { addQuery } from '../utils';

export interface SearchFilter {
    query: string;
    category: (ExtensionCategory | '') & string;
    sortBy: SortBy;
    sortOrder: SortOrder;
}

// Write only non-default values so shared links stay clean.
export function filterToParams({ query, category, sortBy, sortOrder }: SearchFilter): Record<string, string> {
    const params: Record<string, string> = {};
    if (query) params.q = query;
    if (category) params.category = category;
    if (sortBy !== 'relevance') params.sortBy = sortBy;
    if (sortOrder !== 'desc') params.sortOrder = sortOrder;
    return params;
}

export function useSearch() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const { query, setQuery } = useContext(SearchContext);

    const filter: SearchFilter = {
        query: searchParams.get('q') ?? '',
        category: (searchParams.get('category') as ExtensionCategory) ?? '',
        sortBy: (searchParams.get('sortBy') as SortBy) ?? 'relevance',
        sortOrder: (searchParams.get('sortOrder') as SortOrder) ?? 'desc'
    };

    // Keep nav bar in sync with URL query param changes (back/forward, shared links, category tiles).
    useEffect(() => {
        setQuery(filter.query);
    }, [filter.query, setQuery]);

    // On the search page: patch URL params in place (replace, no new history entry).
    // From anywhere else: navigate to the search route.
    const search = useCallback(
        (patch: Partial<SearchFilter>) => {
            const next = { ...filter, ...patch };

            const { pathname } = window.location;
            if (pathname === ExtensionListRoutes.SEARCH) {
                setSearchParams(filterToParams(next), { replace: true });
                return;
            }

            const fromHome = pathname === ExtensionListRoutes.MAIN;
            const url = addQuery(ExtensionListRoutes.SEARCH, [
                { key: 'q', value: next.query || undefined },
                { key: 'category', value: next.category || undefined },
                { key: 'sortBy', value: next.sortBy !== 'relevance' ? next.sortBy : undefined },
                { key: 'sortOrder', value: next.sortOrder !== 'desc' ? next.sortOrder : undefined }
            ]);
            navigate(url, { replace: !fromHome });
        },
        [navigate, filter, setSearchParams]
    );

    return { query, setQuery, search, filter };
}
