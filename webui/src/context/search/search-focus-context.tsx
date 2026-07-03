/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import {
    createContext,
    FunctionComponent,
    ReactNode,
    useCallback,
    useContext,
    useLayoutEffect,
    useMemo,
    useRef,
    useState
} from 'react';
import { GridStep } from '../../hooks/use-grid-cursor';
import { Signal, useSignal } from '../../hooks/use-signal';

/**
 * Focus coordination between the search fields and the results grid. Signals are
 * emitted by whichever component owns the intent; subscribers react via
 * useSignalEffect and focus their own element, so no entry point needs a global
 * DOM lookup.
 */
// Step the results cursor from the search field, or open the card under it.
export type ResultsNavAction = GridStep | 'open';

export interface SearchFocusContextValue {
    // Ask the active search field (the page search bar when one is mounted, nav bar otherwise) to focus.
    searchFocusSignal: Signal;
    // Drive the results grid's cursor while focus stays in the search field.
    resultsNavigationSignal: Signal<ResultsNavAction>;
    // Whether the search field has focus — the grid only shows its cursor then.
    searchFocused: boolean;
    setSearchFocused: (focused: boolean) => void;
    // A page-level search bar (e.g. the home hero) is mounted — the nav bar hides its own field.
    hasPageSearchBar: boolean;
    registerPageSearchBar: () => () => void;
    // Synchronous read — `hasPageSearchBar` lags one render when a bar (un)mounts
    // in the same commit that emits a focus signal.
    isPageSearchBarMounted: () => boolean;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchFocusContext = createContext<SearchFocusContextValue>({
    searchFocusSignal: { signal: 0, emit: () => {} },
    resultsNavigationSignal: { signal: 0, emit: () => {} },
    searchFocused: false,
    setSearchFocused: () => {},
    hasPageSearchBar: false,
    registerPageSearchBar: () => () => {},
    isPageSearchBarMounted: () => false
});

// eslint-disable-next-line react-refresh/only-export-components
export function useSearchFocus(): SearchFocusContextValue {
    return useContext(SearchFocusContext);
}

/**
 * Marks the calling component as the page's search bar while mounted, so the nav
 * bar hides its own field and hands focus requests over.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useRegisterPageSearchBar(): void {
    const { registerPageSearchBar } = useSearchFocus();
    // Layout effect so the count is updated before other layout effects in the same commit.
    useLayoutEffect(registerPageSearchBar, [registerPageSearchBar]);
}

export const SearchFocusProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const searchFocusSignal = useSignal();
    const resultsNavigationSignal = useSignal<ResultsNavAction>();
    const [searchFocused, setSearchFocused] = useState(false);

    // Ref for synchronous reads during a commit; the state mirror drives re-renders.
    const pageSearchBarCount = useRef(0);
    const [hasPageSearchBar, setHasPageSearchBar] = useState(false);
    const registerPageSearchBar = useCallback(() => {
        pageSearchBarCount.current++;
        setHasPageSearchBar(true);
        return () => {
            pageSearchBarCount.current--;
            setHasPageSearchBar(pageSearchBarCount.current > 0);
        };
    }, []);
    const isPageSearchBarMounted = useCallback(() => pageSearchBarCount.current > 0, []);

    const value = useMemo(
        () => ({
            searchFocusSignal,
            resultsNavigationSignal,
            searchFocused,
            setSearchFocused,
            hasPageSearchBar,
            registerPageSearchBar,
            isPageSearchBarMounted
        }),
        [
            searchFocusSignal,
            resultsNavigationSignal,
            searchFocused,
            hasPageSearchBar,
            registerPageSearchBar,
            isPageSearchBarMounted
        ]
    );
    return <SearchFocusContext.Provider value={value}>{children}</SearchFocusContext.Provider>;
};
