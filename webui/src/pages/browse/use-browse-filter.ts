/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useContext, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { MainContext } from '../../context';
import { ExtensionCategory, SortBy, SortOrder } from '../../extension-registry-types';
import { useNavSearch } from '../../nav-search-context';
import { addQuery } from '../../utils';
import { ExtensionListRoutes } from '../extension-list/extension-list-routes';

function buildBrowseUrl(q: string, cat: ExtensionCategory | '', sb: SortBy, so: SortOrder): string {
    const queries: { key: string; value: string }[] = [];
    if (q) queries.push({ key: 'q', value: q });
    if (cat) queries.push({ key: 'category', value: cat });
    if (sb && sb !== 'relevance') queries.push({ key: 'sortBy', value: sb });
    if (so && so !== 'desc') queries.push({ key: 'sortOrder', value: so });
    return addQuery(ExtensionListRoutes.BROWSE, queries);
}

export function useBrowseFilter() {
    const { search, state: locationState } = useLocation();
    const context = useContext(MainContext);
    const { navQuery, setNavQuery, setSearchHandler } = useNavSearch();

    // Navigation state is the most reliable fallback for values passed from HomePage
    // (URL params may not yet be reflected in useLocation() during view transitions)
    const navState = (locationState as { _q?: string; _cat?: string } | null) ?? {};

    const [searchQuery, setSearchQuery] = useState(() => {
        const fromUrl = new URLSearchParams(search).get('q') ?? '';
        return fromUrl || navQuery || navState._q || '';
    });
    const [category, setCategory] = useState<ExtensionCategory | ''>(() => {
        const fromUrl = (new URLSearchParams(search).get('category') as ExtensionCategory) ?? '';
        return fromUrl || (navState._cat as ExtensionCategory) || '';
    });
    const [sortBy, setSortBy] = useState<SortBy>(
        () => (new URLSearchParams(search).get('sortBy') as SortBy) ?? 'relevance'
    );
    const [sortOrder, setSortOrder] = useState<SortOrder>(
        () => (new URLSearchParams(search).get('sortOrder') as SortOrder) ?? 'desc'
    );
    const [debounceTime, setDebounceTime] = useState(0);
    const [resultNumber, setResultNumber] = useState(0);
    const [categories, setCategories] = useState<ExtensionCategory[]>([]);

    // Kept current every render so the stable handler closure always dispatches to latest state
    const handlerRef = useRef<(q: string) => void>(() => {});
    handlerRef.current = (q: string) => {
        setSearchQuery(q);
        setDebounceTime(1000);
        history.replaceState(null, '', buildBrowseUrl(q, category, sortBy, sortOrder));
    };

    // Register this page as the nav bar's search handler; unregister on unmount
    useLayoutEffect(() => {
        const stable = (q: string) => handlerRef.current(q);
        setSearchHandler(stable);
        return () => setSearchHandler(null);
    }, []);

    // On mount: if the URL has a query but navQuery is empty, push it to context so
    // the navbar search bar reflects the current search
    useLayoutEffect(() => {
        if (!navQuery && searchQuery) setNavQuery(searchQuery);
    }, []);

    // Keep local searchQuery in sync when the nav bar drives an external query change.
    // Skip the initial mount: searchQuery is already seeded from the URL/navigation state,
    // and the layout effect above pushes it into navQuery — reacting here on mount would
    // wipe a URL-provided query (e.g. /browse?q=vue) before navQuery has caught up.
    const navSyncMounted = useRef(false);
    useEffect(() => {
        if (!navSyncMounted.current) {
            navSyncMounted.current = true;
            return;
        }
        if (navQuery === searchQuery) return;
        setSearchQuery(navQuery);
        setDebounceTime(500);
        history.replaceState(null, '', buildBrowseUrl(navQuery, category, sortBy, sortOrder));
    }, [navQuery]);

    // Load category list once on mount
    useEffect(() => {
        const cats = Array.from(context.service.getCategories()) as ExtensionCategory[];
        cats.sort((a, b) => {
            if (a === b) return 0;
            if (a === 'Other') return 1;
            if (b === 'Other') return -1;
            return a.localeCompare(b);
        });
        setCategories(cats);
    }, []);

    const updateURL = (q: string, cat: ExtensionCategory | '', sb?: SortBy, so?: SortOrder) => {
        history.replaceState(null, '', buildBrowseUrl(q, cat, sb ?? sortBy, so ?? sortOrder));
    };

    const onSearchChanged = (q: string) => {
        setSearchQuery(q);
        setDebounceTime(1000);
        updateURL(q, category);
    };

    const onCategoryChanged = (cat: ExtensionCategory | '') => {
        setCategory(cat);
        updateURL(searchQuery, cat);
    };

    const onSortByChanged = (sb: SortBy) => {
        setSortBy(sb);
        updateURL(searchQuery, category, sb, sortOrder);
    };

    const onSortOrderChanged = (so: SortOrder) => {
        setSortOrder(so);
        updateURL(searchQuery, category, sortBy, so);
    };

    return {
        searchQuery,
        category,
        sortBy,
        sortOrder,
        debounceTime,
        resultNumber,
        categories,
        onResultCount: setResultNumber,
        onSearchChanged,
        onCategoryChanged,
        onSortByChanged,
        onSortOrderChanged
    };
}
