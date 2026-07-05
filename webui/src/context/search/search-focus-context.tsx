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

import { createContext, FunctionComponent, ReactNode, useContext, useMemo, useState } from 'react';
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

export interface SearchFocusValue {
    // Ask the active search field (the page search bar when one is registered, nav bar otherwise) to focus.
    searchFocusSignal: Signal;
    // Drive the results grid's cursor while focus stays in the search field.
    resultsNavigationSignal: Signal<ResultsNavAction>;
    // Whether the search field has focus — the grid only shows its cursor then.
    searchFocused: boolean;
    setSearchFocused: (focused: boolean) => void;
}

const SearchFocusContext = createContext<SearchFocusValue>({
    searchFocusSignal: { signal: 0, emit: () => {} },
    resultsNavigationSignal: { signal: 0, emit: () => {} },
    searchFocused: false,
    setSearchFocused: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export function useSearchFocus(): SearchFocusValue {
    return useContext(SearchFocusContext);
}

export const SearchFocusProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const searchFocusSignal = useSignal();
    const resultsNavigationSignal = useSignal<ResultsNavAction>();
    const [searchFocused, setSearchFocused] = useState(false);
    const value = useMemo(
        () => ({ searchFocusSignal, resultsNavigationSignal, searchFocused, setSearchFocused }),
        [searchFocusSignal, resultsNavigationSignal, searchFocused]
    );
    return <SearchFocusContext.Provider value={value}>{children}</SearchFocusContext.Provider>;
};
