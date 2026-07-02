/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { createContext, FunctionComponent, ReactNode, useContext, useMemo, useState } from 'react';
import { useSignal } from '../../hooks/use-signal';

/**
 * Focus coordination between the search fields and the results grid. Signals are
 * bumped by whichever component owns the intent; subscribers react via
 * useSignalEffect and focus their own element, so no entry point needs a global
 * DOM lookup.
 */
// Move the results cursor from the search field, or open the card under it.
export type ResultsNavAction = 'down' | 'up' | 'left' | 'right' | 'open';

export interface SearchFocusContextValue {
    // Ask the active search field (hero on the home page, nav bar elsewhere) to focus.
    focusSearchSignal: number;
    focusSearch: () => void;
    // Drive the results grid's cursor while focus stays in the search field.
    resultsNavSignal: number;
    resultsNavAction?: ResultsNavAction;
    navigateResults: (action: ResultsNavAction) => void;
    // Whether the search field has focus — the grid only shows its cursor then.
    searchFocused: boolean;
    setSearchFocused: (focused: boolean) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchFocusContext = createContext<SearchFocusContextValue>({
    focusSearchSignal: 0,
    focusSearch: () => {},
    resultsNavSignal: 0,
    navigateResults: () => {},
    searchFocused: false,
    setSearchFocused: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export function useSearchFocus(): SearchFocusContextValue {
    return useContext(SearchFocusContext);
}

export const SearchFocusProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const focusSearch = useSignal();
    const resultsNav = useSignal<ResultsNavAction>();
    const [searchFocused, setSearchFocused] = useState(false);
    const value = useMemo(
        () => ({
            focusSearchSignal: focusSearch.signal,
            focusSearch: focusSearch.emit,
            resultsNavSignal: resultsNav.signal,
            resultsNavAction: resultsNav.payload,
            navigateResults: resultsNav.emit,
            searchFocused,
            setSearchFocused
        }),
        [focusSearch, resultsNav, searchFocused]
    );
    return <SearchFocusContext.Provider value={value}>{children}</SearchFocusContext.Provider>;
};
