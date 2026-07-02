/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FocusEvent, FunctionComponent, KeyboardEvent, useCallback, useLayoutEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { useLocation } from 'react-router-dom';
import { ExtensionSearchfield } from '../components/extension-searchfield';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { useSearch } from '../hooks/use-search';
import { useSearchQuery } from '../context/search/search-context';
import { ResultsNavAction, useSearchFocus } from '../context/search/search-focus-context';
import { useSignalEffect } from '../hooks/use-signal-effect';
import { useDebouncedCallback } from '../hooks/use-debounced-callback';
import { useShortcut } from '../hooks/use-shortcut';

const ARROW_ACTIONS: Record<string, ResultsNavAction | undefined> = {
    ArrowDown: 'down',
    ArrowUp: 'up',
    ArrowLeft: 'left',
    ArrowRight: 'right'
};

export const NavSearchField: FunctionComponent = () => {
    const { pathname } = useLocation();
    const isHeroPage = pathname === ExtensionListRoutes.MAIN;
    const { query, setQuery } = useSearchQuery();
    const { search, filter } = useSearch();
    const { searchFocusSignal, resultsNavigationSignal, setSearchFocused } = useSearchFocus();
    const inputRef = useRef<HTMLInputElement>(null);

    // Typing debounces navigation; Enter searches immediately. A route change
    // drops any pending navigation (e.g. the user clicked a result mid-debounce).
    const debouncedSearch = useDebouncedCallback(search);
    useLayoutEffect(() => debouncedSearch.cancel, [pathname, debouncedSearch]);

    // On the hero (home) page the field is only a visual placeholder rendered at
    // opacity 0 for the view-transition morph. Mark the subtree `inert` so it is
    // removed from the tab order and the accessibility tree — otherwise keyboard
    // users land on an invisible input. This runs as a layout effect (before the
    // focus signal effect below) so that when navigating away from the hero page
    // the `inert` flag is cleared synchronously, before the focus request fires —
    // otherwise focus would land on a still-inert element and be dropped.
    const fieldRef = useRef<HTMLDivElement>(null);
    useLayoutEffect(() => {
        if (fieldRef.current) {
            fieldRef.current.inert = isHeroPage;
        }
    }, [isHeroPage]);

    // Take focus when requested — except on the hero page, where the hero search
    // field owns focus instead. The onFocus handler moves the cursor to the end.
    useSignalEffect(
        searchFocusSignal,
        useCallback(() => {
            if (isHeroPage) {
                return;
            }
            inputRef.current?.focus({ preventScroll: true });
        }, [isHeroPage])
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

    // On the search page the input and the results share one cursor: arrow keys
    // move it across the grid while focus stays in the field (so they no longer
    // move the text caret there), and Enter opens it. Escape blurs the field.
    const handleInputKeyDown = useCallback(
        (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                (e.target as HTMLInputElement).blur();
                return;
            }
            if (pathname !== ExtensionListRoutes.SEARCH) {
                return;
            }
            const action = ARROW_ACTIONS[e.key];
            if (action) {
                e.preventDefault();
                resultsNavigationSignal.emit(action);
            }
        },
        [resultsNavigationSignal.emit, pathname]
    );

    return (
        <Box
            sx={{
                flex: 1,
                display: { xs: isHeroPage ? 'none' : 'flex', md: 'flex' },
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
                    opacity: isHeroPage ? 0 : 1,
                    pointerEvents: isHeroPage ? 'none' : 'auto',
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
                    viewTransitionName={isHeroPage ? undefined : 'vt-search'}
                    inputProps={{ onFocus: handleInputFocus, onBlur: handleInputBlur, onKeyDown: handleInputKeyDown }}
                />
            </Box>
        </Box>
    );
};
