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

import { FocusEvent, FunctionComponent, KeyboardEvent, useCallback, useLayoutEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { useLocation } from 'react-router-dom';
import { ExtensionSearchfield } from '../components/extension-searchfield';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { useSearch } from '../hooks/use-search';
import { useSearchQuery } from '../context/search/search-context';
import { useSearchFocus } from '../context/search/search-focus-context';
import { useSignalEffect } from '../hooks/use-signal-effect';
import { useDebouncedCallback } from '../hooks/use-debounced-callback';
import { useShortcut } from '../hooks/use-shortcut';

export const NavSearchField: FunctionComponent = () => {
    const { pathname } = useLocation();
    const { query, setQuery } = useSearchQuery();
    const { search, filter } = useSearch();
    const { searchFocusSignal, resultsNavigationSignal, setSearchFocused, hasPageSearchBar, isPageSearchBarMounted } =
        useSearchFocus();
    const inputRef = useRef<HTMLInputElement>(null);

    // Typing debounces navigation; Enter searches immediately. A route change
    // drops any pending navigation (e.g. the user clicked a result mid-debounce).
    const debouncedSearch = useDebouncedCallback(search);
    useLayoutEffect(() => debouncedSearch.cancel, [pathname, debouncedSearch]);

    // While a page search bar is mounted, this field is only an opacity-0
    // placeholder for the view-transition morph; `inert` removes it from the tab
    // order and accessibility tree. `pendingFocus` holds a focus request that
    // arrived in the commit that unmounted the page search bar (before this
    // component re-rendered); it is honored here once the field is visible again.
    const fieldRef = useRef<HTMLDivElement>(null);
    const pendingFocus = useRef(false);
    useLayoutEffect(() => {
        if (fieldRef.current) {
            fieldRef.current.inert = hasPageSearchBar;
        }
        if (!hasPageSearchBar && pendingFocus.current) {
            pendingFocus.current = false;
            inputRef.current?.focus({ preventScroll: true });
        }
    }, [hasPageSearchBar]);

    // Take focus when requested — unless a page search bar is mounted and owns focus instead.
    useSignalEffect(
        searchFocusSignal,
        useCallback(() => {
            if (isPageSearchBarMounted()) {
                return;
            }
            if (hasPageSearchBar) {
                // Stale commit: the bar just unmounted; defer to the layout effect above.
                pendingFocus.current = true;
                return;
            }
            inputRef.current?.focus({ preventScroll: true });
        }, [isPageSearchBarMounted, hasPageSearchBar])
    );

    // The '/' shortcut asks whichever search field is active to take focus.
    useShortcut({ key: '/', label: 'Focus search', order: 1, callback: searchFocusSignal.emit });

    const handleNavSearch = useCallback(
        (q: string) => {
            setQuery(q);
            debouncedSearch({ query: q });
        },
        [setQuery, debouncedSearch]
    );

    const handleNavSubmit = useCallback(
        (q: string) => {
            debouncedSearch.cancel();
            // On the search page, Enter on an already-applied query opens the card
            // under the cursor; on a fresh query it applies the search first.
            if (pathname === ExtensionListRoutes.SEARCH && q === filter.query) {
                resultsNavigationSignal.emit('open');
                return;
            }
            search({ query: q });
        },
        [debouncedSearch, search, pathname, filter.query, resultsNavigationSignal.emit]
    );

    // Move cursor to end when the input gains focus (e.g. after view-transition morphs
    // the hero search into this field — browsers select-all by default on programmatic focus)
    const handleInputFocus = useCallback(
        (e: FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
            setSearchFocused(true);
            const { target } = e;
            requestAnimationFrame(() => target.setSelectionRange(target.value.length, target.value.length));
        },
        [setSearchFocused]
    );

    const handleInputBlur = useCallback(() => setSearchFocused(false), [setSearchFocused]);

    // On the search page the input and the results share one cursor: Up/Down
    // step it through the results item by item while focus (and the text caret)
    // stays in the field, and Enter opens it. Escape blurs the field.
    const handleInputKeyDown = useCallback(
        (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                (e.target as HTMLInputElement).blur();
                return;
            }
            if (pathname !== ExtensionListRoutes.SEARCH) {
                return;
            }
            if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                e.preventDefault();
                resultsNavigationSignal.emit(e.key === 'ArrowDown' ? 'next' : 'previous');
            }
        },
        [resultsNavigationSignal.emit, pathname]
    );

    return (
        <Box
            sx={{
                flex: 1,
                display: { xs: hasPageSearchBar ? 'none' : 'flex', md: 'flex' },
                alignItems: 'center',
                justifyContent: 'center',
                px: { xs: '0.5rem', md: '1.25rem' }
            }}>
            <Box
                ref={fieldRef}
                sx={{
                    display: 'flex',
                    width: '100%',
                    maxWidth: '35rem',
                    mx: 'auto',
                    opacity: hasPageSearchBar ? 0 : 1,
                    pointerEvents: hasPageSearchBar ? 'none' : 'auto',
                    transition: 'opacity 0.15s ease'
                }}>
                <ExtensionSearchfield
                    ref={inputRef}
                    onSearchChanged={handleNavSearch}
                    onSearchSubmit={handleNavSubmit}
                    searchQuery={query}
                    placeholder='search extensions…'
                    hideIconButton
                    autoFocus={false}
                    viewTransitionName={hasPageSearchBar ? undefined : 'vt-search'}
                    inputProps={{ onFocus: handleInputFocus, onBlur: handleInputBlur, onKeyDown: handleInputKeyDown }}
                />
            </Box>
        </Box>
    );
};
