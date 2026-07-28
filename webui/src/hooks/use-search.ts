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

import { createContext, useContext, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { ExtensionCategory, SortBy, SortOrder } from '../extension-registry-types';

export interface SearchFilter {
    query: string;
    category: (ExtensionCategory | '') & string;
    sortBy: SortBy;
    sortOrder: SortOrder;
}

/** Debounce for the extension search field — short enough to feel instant while still batching fast typing. */
export const SEARCH_DEBOUNCE_MS = 150;

export function filterFromParams(searchParams: URLSearchParams): SearchFilter {
    return {
        query: searchParams.get('q') ?? '',
        category: (searchParams.get('category') as ExtensionCategory) ?? '',
        sortBy: (searchParams.get('sortBy') as SortBy) ?? 'relevance',
        sortOrder: (searchParams.get('sortOrder') as SortOrder) ?? 'desc'
    };
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

export interface SearchOptions {
    /** Apply after the typing debounce; an empty query then only re-searches on the search page. */
    debounce?: boolean;
    /** Replace the history entry instead of pushing — for redirects that shouldn't be reachable via Back. */
    replace?: boolean;
}

export type SearchAction = (patch: Partial<SearchFilter>, options?: SearchOptions) => void;

/** Implemented by SearchProvider, which owns the draft ↔ URL bridge; no-op outside it. */
export const SearchActionContext = createContext<SearchAction>(() => {});

/**
 * The applied search filter (from the URL) and the `search` action, which
 * applies a filter patch: it updates the fields' draft and navigates to the
 * search page.
 */
export function useSearch() {
    const [searchParams] = useSearchParams();
    // Memoized so `filter` keeps its identity between renders of the same URL.
    const filter: SearchFilter = useMemo(() => filterFromParams(searchParams), [searchParams]);
    const search = useContext(SearchActionContext);
    return { search, filter };
}
