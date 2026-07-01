/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useCallback, useLayoutEffect } from 'react';
import { flushSync } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { useNavSearch } from '../../nav-search-context';
import { ExtensionListRoutes } from '../extension-list/extension-list-routes';
import { addQuery } from '../../utils';
import { HomepageView } from './homepage-view';

export const HomePage: FunctionComponent = () => {
    const { setIsHeroPage, setNavQuery } = useNavSearch();
    const navigate = useNavigate();

    useLayoutEffect(() => {
        setIsHeroPage(true);
        return () => setIsHeroPage(false);
    }, []);

    const focusNavSearch = useCallback(() => {
        const input = document.getElementById('search-input') as HTMLInputElement | null;
        if (input) {
            input.focus();
            input.select();
        }
    }, []);

    const handleSearch = useCallback(
        (query: string, category?: string) => {
            const cat = category ?? '';
            const queries: { key: string; value: string }[] = [];
            if (query) queries.push({ key: 'q', value: query });
            if (cat) queries.push({ key: 'category', value: cat });
            const url = addQuery(ExtensionListRoutes.BROWSE, queries);

            if ('startViewTransition' in document) {
                const transition = (document as any).startViewTransition(() => {
                    flushSync(() => {
                        setNavQuery(query);
                        setIsHeroPage(false);
                        navigate(url, { state: { _q: query, _cat: cat } });
                    });
                });
                transition.finished.then(focusNavSearch);
            } else {
                setNavQuery(query);
                navigate(url, { state: { _q: query, _cat: cat } });
                requestAnimationFrame(() => requestAnimationFrame(focusNavSearch));
            }
        },
        [navigate, setNavQuery, setIsHeroPage, focusNavSearch]
    );

    return <HomepageView onSearch={handleSearch} />;
};
