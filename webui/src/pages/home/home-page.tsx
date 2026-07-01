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
        // preventScroll: a plain focus() scrolls the input into view, which on mobile
        // fights the scroll-to-top and can leave the page sitting over the results.
        input?.focus({ preventScroll: true });
    }, []);

    const handleSearch = useCallback(
        (query: string, category?: string) => {
            const cat = category ?? '';
            const queries: { key: string; value: string }[] = [];
            if (query) queries.push({ key: 'q', value: query });
            if (cat) queries.push({ key: 'category', value: cat });
            const url = addQuery(ExtensionListRoutes.SEARCH, queries);

            const run = () => {
                setNavQuery(query);
                setIsHeroPage(false);
                navigate(url, { state: { _q: query, _cat: cat } });
            };

            // Only steal focus into the search bar for an actual text search — not when
            // navigating to /search by browsing a category or "view all".
            const shouldFocus = Boolean(query);

            if ('startViewTransition' in document) {
                const transition = (document as any).startViewTransition(() => {
                    flushSync(run);
                    // Move focus to the nav search field synchronously, while the hero
                    // input is still focused, so the mobile keyboard stays open. Waiting
                    // for transition.finished leaves a gap that drops the keyboard.
                    if (shouldFocus) focusNavSearch();
                });
                if (shouldFocus) transition.finished.then(focusNavSearch);
            } else {
                flushSync(run);
                if (shouldFocus) focusNavSearch();
            }
        },
        [navigate, setNavQuery, setIsHeroPage, focusNavSearch]
    );

    return <HomepageView onSearch={handleSearch} />;
};
