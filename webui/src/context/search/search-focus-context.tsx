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
    RefObject,
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
    // A page-level search bar (e.g. the home hero) is registered — the nav bar hides its own field.
    hasPageSearchBar: boolean;
    registerPageSearchBar: () => () => void;
    // Synchronous read — `hasPageSearchBar` lags one render when a bar (un)registers
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
 * Marks the calling component as the page's search bar, so the nav bar hides its
 * own field and hands focus requests over. Registered while mounted, or — when
 * `visibilityRef` is given — only while that element is in the viewport. Returns
 * whether the bar is currently registered; callers gate focus handling and their
 * view-transition name on it.
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useRegisterPageSearchBar(visibilityRef?: RefObject<Element>): boolean {
    const { registerPageSearchBar } = useSearchFocus();
    const [registered, setRegistered] = useState(true);
    // Layout effect so the count is updated before other layout effects in the same commit.
    useLayoutEffect(() => {
        const el = visibilityRef?.current;
        if (!el) {
            return registerPageSearchBar();
        }
        let unregister: (() => void) | undefined;
        const update = (visible: boolean) => {
            if (visible && !unregister) {
                unregister = registerPageSearchBar();
            } else if (!visible && unregister) {
                unregister();
                unregister = undefined;
            }
            setRegistered(visible);
        };
        // Seed synchronously — the observer's first callback is async and the nav field would flash.
        const rect = el.getBoundingClientRect();
        update(rect.bottom > 0 && rect.top < window.innerHeight && rect.right > 0 && rect.left < window.innerWidth);
        const observer = new IntersectionObserver(entries => update(entries[entries.length - 1].isIntersecting));
        observer.observe(el);
        return () => {
            observer.disconnect();
            unregister?.();
        };
    }, [registerPageSearchBar, visibilityRef]);
    return registered;
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
