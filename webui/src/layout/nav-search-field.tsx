/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, KeyboardEvent, useCallback, useLayoutEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { useLocation } from 'react-router-dom';
import { ExtensionSearchfield } from '../components/extension-searchfield';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { useSearch } from '../hooks/use-search';
import { useSearchFocus } from '../hooks/use-search-focus';
import { useSignalEffect } from '../hooks/use-signal-effect';
import { useDebouncedCallback } from '../hooks/use-debounced-callback';
import { useShortcut } from '../use-shortcut';

export const NavSearchField: FunctionComponent = () => {
    const { pathname } = useLocation();
    const isHeroPage = pathname === ExtensionListRoutes.MAIN;
    const { query, setQuery, search } = useSearch();
    const { focusSearchSignal, focusSearch, focusResults } = useSearchFocus();
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
        focusSearchSignal,
        useCallback(() => {
            if (isHeroPage) {
                return;
            }
            inputRef.current?.focus({ preventScroll: true });
        }, [isHeroPage])
    );

    // The '/' shortcut asks whichever search field is active to take focus.
    useShortcut({ key: '/', label: 'Focus search', order: 1, callback: focusSearch });

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
            search({ query: q });
        },
        [debouncedSearch, search]
    );

    // Move cursor to end when the input gains focus (e.g. after view-transition morphs
    // the hero search into this field — browsers select-all by default on programmatic focus)
    const handleInputFocus = useCallback((e: { target: HTMLInputElement }) => {
        const { target } = e;
        requestAnimationFrame(() => target.setSelectionRange(target.value.length, target.value.length));
    }, []);

    // ArrowDown moves focus from the search field into the results grid (first card),
    // where two-axis arrow navigation takes over. Escape blurs the field.
    const handleInputKeyDown = useCallback(
        (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                (e.target as HTMLInputElement).blur();
                return;
            }
            if (e.key !== 'ArrowDown') {
                return;
            }
            e.preventDefault();
            focusResults();
        },
        [focusResults]
    );

    return (
        <Box
            sx={{
                flex: 1,
                display: { xs: isHeroPage ? 'none' : 'flex', md: 'flex' },
                alignItems: 'center',
                justifyContent: 'center',
                px: { xs: '8px', md: '20px' }
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
                    transition: 'opacity 0.15s ease',
                    '& > *': { mb: '0 !important' }
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
                    inputProps={{ onFocus: handleInputFocus, onKeyDown: handleInputKeyDown }}
                />
            </Box>
        </Box>
    );
};
