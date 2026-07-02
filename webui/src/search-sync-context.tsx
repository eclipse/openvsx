/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { createContext, FunctionComponent, ReactNode, useCallback, useContext, useMemo, useState } from 'react';

interface SearchSyncContextValue {
    navQuery: string;
    setNavQuery: (q: string) => void;
    searchHandler: ((q: string) => void) | null;
    setSearchHandler: (fn: ((q: string) => void) | null) => void;
}

const SearchSyncContext = createContext<SearchSyncContextValue>({
    navQuery: '',
    setNavQuery: () => {},
    searchHandler: null,
    setSearchHandler: () => {}
});

export const SearchSyncProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [navQuery, setNavQuery] = useState('');
    // useState treats any function passed directly as a reducer — wrap with () => fn to store functions correctly
    const [searchHandler, setSearchHandlerState] = useState<((q: string) => void) | null>(null);
    const setSearchHandler = useCallback((fn: ((q: string) => void) | null) => {
        setSearchHandlerState(() => fn);
    }, []);
    const value = useMemo(
        () => ({ navQuery, setNavQuery, searchHandler, setSearchHandler }),
        [navQuery, searchHandler, setSearchHandler]
    );
    return <SearchSyncContext.Provider value={value}>{children}</SearchSyncContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useSearchSync = () => useContext(SearchSyncContext);
