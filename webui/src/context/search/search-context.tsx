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

import { createContext, FunctionComponent, ReactNode, useContext, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SearchFocusProvider } from './search-focus-context';
import { PageSearchBarProvider } from './page-search-bar-context';

export interface SearchContextValue {
    query: string;
    setQuery: (q: string) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchContext = createContext<SearchContextValue>({
    query: '',
    setQuery: () => {}
});

/**
 * The draft query shared by the search fields. Only the fields themselves should
 * read this (it changes per keystroke); pages read the applied filter from the
 * URL via useSearch instead.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useSearchQuery(): SearchContextValue {
    return useContext(SearchContext);
}

// Mounts the whole search feature: the persisted query (so the search fields stay
// in sync as the user navigates) plus the focus and page-bar contexts. Separate
// contexts keep re-render scopes tight — the query changes per keystroke, the
// others rarely. The `search` action lives in useSearch.
export const SearchProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [query, setQuery] = useState('');
    const [searchParams] = useSearchParams();
    const urlQuery = searchParams.get('q') ?? '';

    // Keep the fields in sync with URL query changes (back/forward, shared links, category tiles).
    useEffect(() => {
        setQuery(urlQuery);
    }, [urlQuery]);

    const value = useMemo(() => ({ query, setQuery }), [query]);
    return (
        <SearchContext.Provider value={value}>
            <SearchFocusProvider>
                <PageSearchBarProvider>{children}</PageSearchBarProvider>
            </SearchFocusProvider>
        </SearchContext.Provider>
    );
};
