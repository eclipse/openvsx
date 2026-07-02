/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, KeyboardEvent, useCallback, useEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { ExtensionSearchfield } from '../components/extension-searchfield';
import { useNavSearch } from '../nav-search-context';
import { useShortcut } from '../use-shortcut';

export const NavSearchField: FunctionComponent = () => {
    const { isHeroPage, navQuery, setNavQuery, searchHandler } = useNavSearch();
    const navigate = useNavigate();

    // Keep a ref so handleNavSearch always calls the latest handler without needing
    // to be recreated every time searchHandler changes (avoids stale-closure navigates)
    const searchHandlerRef = useRef(searchHandler);
    searchHandlerRef.current = searchHandler;

    // On the hero (home) page the field is only a visual placeholder rendered at
    // opacity 0 for the view-transition morph. Mark the subtree `inert` so it is
    // removed from the tab order and the accessibility tree — otherwise keyboard
    // users land on an invisible input.
    const fieldRef = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (fieldRef.current) {
            fieldRef.current.inert = isHeroPage;
        }
    }, [isHeroPage]);

    const focusSearch = useCallback(() => {
        const hero = document.getElementById('hero-search-input') as HTMLInputElement | null;
        if (hero) {
            hero.focus();
            // Move cursor to end so the user can continue typing after transitioning back
            requestAnimationFrame(() => hero.setSelectionRange(hero.value.length, hero.value.length));
            return;
        }
        const nav = document.getElementById('search-input') as HTMLInputElement | null;
        if (nav) {
            nav.focus();
            requestAnimationFrame(() => nav.setSelectionRange(nav.value.length, nav.value.length));
        }
    }, []);

    useShortcut({ key: '/', label: 'Focus search', order: 1, callback: focusSearch });

    const handleNavSearch = useCallback(
        (q: string) => {
            setNavQuery(q);
            if (searchHandlerRef.current) {
                searchHandlerRef.current(q);
            } else {
                navigate(`/search${q ? '?q=' + encodeURIComponent(q) : ''}`);
            }
        },
        [navigate, setNavQuery]
    );

    // Move cursor to end when the input gains focus (e.g. after view-transition morphs
    // the hero search into this field — browsers select-all by default on programmatic focus)
    const handleInputFocus = useCallback((e: { target: HTMLInputElement }) => {
        const { target } = e;
        requestAnimationFrame(() => target.setSelectionRange(target.value.length, target.value.length));
    }, []);

    // ArrowDown moves focus from the search field into the results grid (first card),
    // where two-axis arrow navigation takes over. Escape blurs the field.
    const handleInputKeyDown = useCallback((e: KeyboardEvent) => {
        if (e.key === 'Escape') {
            (e.target as HTMLInputElement).blur();
            return;
        }
        if (e.key !== 'ArrowDown') return;
        const firstCard = document.querySelector('[data-grid-item], a[data-ext-card]') as HTMLElement | null;
        if (firstCard) {
            e.preventDefault();
            firstCard.focus();
        }
    }, []);

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
                    onSearchChanged={handleNavSearch}
                    searchQuery={navQuery}
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
