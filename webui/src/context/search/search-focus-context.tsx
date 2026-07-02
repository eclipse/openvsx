/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { createContext, FunctionComponent, ReactNode, useContext, useMemo } from 'react';
import { useSignal } from '../../hooks/use-signal';

/**
 * Focus coordination between the search fields and the results grid. Signals are
 * bumped by whichever component owns the intent; subscribers react via
 * useSignalEffect and focus their own element, so no entry point needs a global
 * DOM lookup.
 */
export interface SearchFocusContextValue {
    // Ask the active search field (hero on the home page, nav bar elsewhere) to focus.
    focusSearchSignal: number;
    focusSearch: () => void;
    // Ask the results grid to focus its first item.
    focusResultsSignal: number;
    focusResults: () => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchFocusContext = createContext<SearchFocusContextValue>({
    focusSearchSignal: 0,
    focusSearch: () => {},
    focusResultsSignal: 0,
    focusResults: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export function useSearchFocus(): SearchFocusContextValue {
    return useContext(SearchFocusContext);
}

export const SearchFocusProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const focusSearch = useSignal();
    const focusResults = useSignal();
    const value = useMemo(
        () => ({
            focusSearchSignal: focusSearch.signal,
            focusSearch: focusSearch.emit,
            focusResultsSignal: focusResults.signal,
            focusResults: focusResults.emit
        }),
        [focusSearch, focusResults]
    );
    return <SearchFocusContext.Provider value={value}>{children}</SearchFocusContext.Provider>;
};
