/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { createContext, FunctionComponent, ReactNode, useMemo, useState } from 'react';

export interface SearchContextValue {
    query: string;
    setQuery: (q: string) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const SearchContext = createContext<SearchContextValue>({
    query: '',
    setQuery: () => {}
});

// Holds the persisted search query so the search fields stay in sync as the user
// navigates. The `search` action lives in useSearch; focus coordination lives in
// SearchFocusContext.
export const SearchProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [query, setQuery] = useState('');
    const value = useMemo(() => ({ query, setQuery }), [query]);
    return <SearchContext.Provider value={value}>{children}</SearchContext.Provider>;
};
