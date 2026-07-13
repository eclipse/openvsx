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

import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { ExtensionCategory, SortBy, SortOrder } from '../extension-registry-types';

export interface SearchFilter {
    query: string;
    category: (ExtensionCategory | '') & string;
    sortBy: SortBy;
    sortOrder: SortOrder;
}

/** Debounce for the extension search field — short enough to feel instant while still batching fast typing. */
export const SEARCH_DEBOUNCE_MS = 150;

// Write only non-default values so shared links stay clean.
function filterToParams({ query, category, sortBy, sortOrder }: SearchFilter): Record<string, string> {
    const params: Record<string, string> = {};
    if (query) params.q = query;
    if (category) params.category = category;
    if (sortBy !== 'relevance') params.sortBy = sortBy;
    if (sortOrder !== 'desc') params.sortOrder = sortOrder;
    return params;
}

/** The applied search filter (from the URL) and the `search` navigation action. */
export function useSearch() {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [searchParams, setSearchParams] = useSearchParams();

    // Memoized so `filter` and `search` keep their identity between renders of the same URL.
    const filter: SearchFilter = useMemo(
        () => ({
            query: searchParams.get('q') ?? '',
            category: (searchParams.get('category') as ExtensionCategory) ?? '',
            sortBy: (searchParams.get('sortBy') as SortBy) ?? 'relevance',
            sortOrder: (searchParams.get('sortOrder') as SortOrder) ?? 'desc'
        }),
        [searchParams]
    );

    // On the search page: patch URL params in place (replace, no new history entry).
    // From anywhere else: push a navigation to the search route.
    const search = useCallback(
        (patch: Partial<SearchFilter>) => {
            const next = { ...filter, ...patch };
            // Trim here so every entry point (nav field, hero, chips) searches the same way.
            next.query = next.query.trim();
            const params = filterToParams(next);

            if (pathname === ExtensionListRoutes.SEARCH) {
                setSearchParams(params, { replace: true });
                return;
            }

            navigate({ pathname: ExtensionListRoutes.SEARCH, search: new URLSearchParams(params).toString() });
        },
        [navigate, pathname, filter, setSearchParams]
    );

    return { search, filter };
}
