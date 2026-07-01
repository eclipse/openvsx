/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { createContext, FunctionComponent, ReactNode, useCallback, useContext, useState } from 'react';

interface NavSearchContextValue {
    isHeroPage: boolean;
    setIsHeroPage: (b: boolean) => void;
    navQuery: string;
    setNavQuery: (q: string) => void;
    searchHandler: ((q: string) => void) | null;
    setSearchHandler: (fn: ((q: string) => void) | null) => void;
}

const NavSearchContext = createContext<NavSearchContextValue>({
    isHeroPage: false,
    setIsHeroPage: () => {},
    navQuery: '',
    setNavQuery: () => {},
    searchHandler: null,
    setSearchHandler: () => {}
});

export const NavSearchProvider: FunctionComponent<{ children: ReactNode }> = ({ children }) => {
    const [isHeroPage, setIsHeroPage] = useState(false);
    const [navQuery, setNavQuery] = useState('');
    // useState treats any function passed directly as a reducer — wrap with () => fn to store functions correctly
    const [searchHandler, setSearchHandlerState] = useState<((q: string) => void) | null>(null);
    const setSearchHandler = useCallback((fn: ((q: string) => void) | null) => {
        setSearchHandlerState(() => fn);
    }, []);
    return (
        <NavSearchContext.Provider
            value={{ isHeroPage, setIsHeroPage, navQuery, setNavQuery, searchHandler, setSearchHandler }}>
            {children}
        </NavSearchContext.Provider>
    );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useNavSearch = () => useContext(NavSearchContext);
