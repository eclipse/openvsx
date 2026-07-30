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

import {
    createContext,
    FunctionComponent,
    ReactNode,
    useCallback,
    useContext,
    useEffect,
    useLayoutEffect,
    useMemo,
    useState
} from 'react';
import { useLocation, useNavigate, useNavigationType, useSearchParams } from 'react-router';
import {
    filterFromParams,
    filterToParams,
    SearchActionContext,
    SearchFilter,
    SearchOptions,
    SEARCH_DEBOUNCE_MS
} from '../../hooks/use-search';
import { useDebouncedCallback } from '../../hooks/use-debounced-callback';
import { ExtensionListRoutes } from '../../pages/extension-list/extension-list-routes';
import { SearchFocusProvider, useSearchFocus } from './search-focus-context';
import { PageSearchBarProvider, usePageSearchBar } from './page-search-bar-context';
import { resolveSearchViewTransition, startSearchViewTransition } from './search-view-transition';

export interface SearchContextValue {
    query: string;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchContext = createContext<SearchContextValue>({ query: '' });

/**
 * The draft query shown in the search fields. Read-only — written by the
 * `search` action and by URL changes; pages read the applied filter via useSearch.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useSearchQuery(): SearchContextValue {
    return useContext(SearchContext);
}

/**
 * Owns the draft query and keeps it in step with the URL. `search` updates the
 * draft and navigates, immediately or through one shared typing debounce (so a
 * pending apply survives the hero → nav morph). The draft follows the inputs;
 * the URL is synced back into it only on history navigation (back/forward,
 * initial load), never on our own writes — react-router commits those in a
 * transition, so a late echo would otherwise snap the field to a stale value.
 */
const SearchQueryProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [query, setQueryState] = useState('');
    const navigate = useNavigate();
    const location = useLocation();
    const navigationType = useNavigationType();
    const { pathname } = location;
    const [searchParams, setSearchParams] = useSearchParams();
    const urlQuery = searchParams.get('q') ?? '';
    const { isPageSearchBarActive } = usePageSearchBar();
    const { searchFocusSignal } = useSearchFocus();

    // Release the hero → nav morph once the navigation has committed.
    useLayoutEffect(resolveSearchViewTransition, [location]);

    // URL → draft: only follow POP (back/forward and initial load). Every in-app
    // change goes through `search`, which sets the draft itself, so our own
    // PUSH/REPLACE navigations must not feed back — that's what caused the dance.
    useEffect(() => {
        if (navigationType === 'POP') {
            setQueryState(urlQuery);
        }
    }, [urlQuery, navigationType]);

    // Draft → URL: navigate to the patched filter.
    const applySearch = useCallback(
        (patch: Partial<SearchFilter>, options?: SearchOptions) => {
            const next = { ...filterFromParams(searchParams), ...patch };
            // Trim here so every entry point (fields, chips, tiles) searches the same way.
            next.query = next.query.trim();

            const params = filterToParams(next);
            const go = () =>
                pathname === ExtensionListRoutes.SEARCH
                    ? setSearchParams(params, { replace: true })
                    : navigate(
                          { pathname: ExtensionListRoutes.SEARCH, search: new URLSearchParams(params).toString() },
                          { replace: options?.replace }
                      );

            if (pathname !== ExtensionListRoutes.SEARCH && isPageSearchBarActive()) {
                // The hero morph: navigate inside a view transition and hand focus to
                // the nav field, re-issuing once the morph settles (it can interrupt the first).
                const shouldFocus = Boolean(next.query);
                const transition = startSearchViewTransition(() => {
                    go();
                    if (shouldFocus) searchFocusSignal.emit();
                });
                if (shouldFocus) {
                    transition?.finished.then(searchFocusSignal.emit).catch(() => undefined);
                }
                return;
            }
            go();
        },
        [searchParams, pathname, navigate, setSearchParams, isPageSearchBarActive, searchFocusSignal.emit]
    );

    const debouncedApply = useDebouncedCallback(applySearch, SEARCH_DEBOUNCE_MS);

    // A pending apply follows the user onto the search page (mid-morph keystrokes);
    // any other route change drops it (e.g. a result was clicked mid-debounce).
    useLayoutEffect(() => {
        if (pathname !== ExtensionListRoutes.SEARCH) {
            debouncedApply.cancel();
        }
    }, [pathname, debouncedApply]);

    // The single write path: a query patch updates the draft right away, the
    // navigation runs now or after the debounce. While typing, an empty query
    // only re-searches on the search page — elsewhere it must not navigate away.
    const search = useCallback(
        (patch: Partial<SearchFilter>, options?: SearchOptions) => {
            if (patch.query !== undefined) {
                setQueryState(patch.query);
            }
            if (options?.debounce) {
                if (patch.query !== undefined && !patch.query.trim() && pathname !== ExtensionListRoutes.SEARCH) {
                    debouncedApply.cancel();
                    return;
                }
                debouncedApply(patch);
                return;
            }
            debouncedApply.cancel();
            applySearch(patch, options);
        },
        [debouncedApply, applySearch, pathname]
    );

    const value = useMemo(() => ({ query }), [query]);
    return (
        <SearchContext.Provider value={value}>
            <SearchActionContext.Provider value={search}>{children}</SearchActionContext.Provider>
        </SearchContext.Provider>
    );
};

// Mounts the whole search feature; the query provider sits innermost because
// it consumes the other contexts.
export const SearchProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => (
    <SearchFocusProvider>
        <PageSearchBarProvider>
            <SearchQueryProvider>{children}</SearchQueryProvider>
        </PageSearchBarProvider>
    </SearchFocusProvider>
);
