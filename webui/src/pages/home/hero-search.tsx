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

import {
    ChangeEvent,
    ComponentType,
    FormEvent,
    FunctionComponent,
    useCallback,
    useLayoutEffect,
    useRef,
    useState
} from 'react';
import { Box, ButtonBase, Typography } from '@mui/material';
import { useLocation } from 'react-router-dom';
import { flushSync } from 'react-dom';
import { styled, alpha } from '@mui/material/styles';
import { accentHover, focusOutline, focusRing, Section } from '../../components/page-primitives';
import { useSearch, SEARCH_DEBOUNCE_MS } from '../../hooks/use-search';
import { useSearchQuery } from '../../context/search/search-context';
import { useSearchFocus } from '../../context/search/search-focus-context';
import { useRegisterPageSearchBar } from '../../context/search/page-search-bar-context';
import { useSignalEffect } from '../../hooks/use-signal-effect';
import { useDebouncedCallback } from '../../hooks/use-debounced-callback';
import { MONO_FONT } from '../../default/theme';

const HeroSearchWrap = styled(Box)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.8125rem',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '15px',
    height: '3.875rem',
    paddingLeft: '1.25rem',
    paddingRight: '0.5rem',
    [theme.breakpoints.down('sm')]: {
        height: '3.375rem',
        paddingLeft: '0.875rem',
        gap: '0.625rem'
    },
    boxShadow: 'var(--shadow)',
    transition: 'border-color 0.2s ease, box-shadow 0.3s ease',
    '&:focus-within': focusRing(theme, `0 18px 70px -10px ${alpha(theme.palette.secondary.main, 0.45)}`)
}));

const HeroInput = styled('input')(({ theme }) => ({
    flex: 1,
    height: '100%',
    border: 'none',
    outline: 'none',
    background: 'none',
    color: theme.palette.text.primary,
    fontSize: '1.0625rem',
    fontFamily: MONO_FONT,
    '&::placeholder': { color: theme.palette.text.disabled }
}));

const HeroSubmitButton = styled(ButtonBase)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    height: '2.875rem',
    padding: '0 1.375rem',
    borderRadius: '11px',
    overflow: 'hidden',
    backgroundColor: theme.palette.secondary.main,
    color: theme.palette.secondary.contrastText,
    fontSize: '0.9375rem',
    fontWeight: 600,
    flexShrink: 0,
    transition: 'background 0.14s',
    [theme.breakpoints.down('sm')]: {
        height: '2.5rem',
        padding: '0 0.875rem',
        borderRadius: `${theme.shape.borderRadius}px`
    },
    '&:hover': { backgroundColor: theme.palette.secondary.dark },
    ...focusOutline(theme)
}));

const PopularChip = styled(ButtonBase)(({ theme }) => ({
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    color: theme.palette.text.secondary,
    fontSize: '0.8125rem',
    fontWeight: 500,
    padding: '0.375rem 0.8125rem',
    borderRadius: '999px',
    overflow: 'hidden',
    fontFamily: MONO_FONT,
    transition: 'border-color 0.14s, color 0.14s',
    ...accentHover(theme),
    ...focusOutline(theme)
}));

export interface HeroSearchProps {
    /** Rendered above the search field (headline, tagline, …). */
    searchHeader?: ComponentType;
    /** Search terms offered as one-click chips below the field. */
    popularSearches?: string[];
}

/**
 * The hero search section. Registers itself as the page's search bar while in
 * view, so the nav bar hides its own field. Owns a local copy of `query` so
 * keystrokes only re-render the field, not the whole homepage; typing debounces
 * `search`, submit and popular chips search immediately.
 */
export const HeroSearch: FunctionComponent<HeroSearchProps> = ({
    searchHeader: SearchHeader,
    popularSearches = []
}) => {
    const { query: contextQuery, setQuery } = useSearchQuery();
    const { search } = useSearch();
    const { searchFocusSignal, searchFocused } = useSearchFocus();
    const [query, setLocalQuery] = useState('');
    const heroInputRef = useRef<HTMLInputElement>(null);
    const isActiveSearchBar = useRegisterPageSearchBar(heroInputRef);

    // Focus the hero input on request (e.g. the '/' shortcut) — the nav field owns focus while the hero is out of view.
    useSignalEffect(
        searchFocusSignal,
        useCallback(() => {
            const el = heroInputRef.current;
            if (!el || !isActiveSearchBar) {
                return;
            }
            el.focus();
            // Move cursor to end so the user can keep typing.
            requestAnimationFrame(() => el.setSelectionRange(el.value.length, el.value.length));
        }, [isActiveSearchBar])
    );

    // Reconcile the scroll swap: the field taking over adopts the shared draft and
    // takes focus if its counterpart had it (the signal routes to the active bar).
    const wasActiveSearchBar = useRef(isActiveSearchBar);
    useLayoutEffect(() => {
        if (wasActiveSearchBar.current === isActiveSearchBar) {
            return;
        }
        wasActiveSearchBar.current = isActiveSearchBar;
        if (isActiveSearchBar) {
            setLocalQuery(contextQuery);
            if (searchFocused) searchFocusSignal.emit();
        } else {
            setQuery(query);
            if (document.activeElement === heroInputRef.current) searchFocusSignal.emit();
        }
    }, [isActiveSearchBar, searchFocused, contextQuery, query, setQuery, searchFocusSignal.emit]);

    // Focus the search by default when the hero page is the app's landing page.
    const { key: locationKey } = useLocation();
    useLayoutEffect(() => {
        if (locationKey === 'default') {
            searchFocusSignal.emit();
        }
    }, [locationKey, searchFocusSignal.emit]);

    // Wrap search calls with a view transition so the hero input morphs into the nav bar.
    type ViewTransitionDocument = Document & {
        startViewTransition?: (callback: () => void) => { finished: Promise<void> };
    };

    const searchWithTransition = useCallback(
        (q: string) => {
            // Sync context immediately so the nav bar input value is ready before the
            // navigation commits (the hero input morphs into the nav field mid-transition).
            setQuery(q);
            const shouldFocus = Boolean(q);
            // flushSync inside the transition commits the navigation and the focus signal
            // synchronously, so the nav field takes focus while the hero input is still
            // focused — keeping the mobile keyboard open across the morph.
            const go = () => {
                flushSync(() => {
                    search({ query: q });
                    if (shouldFocus) searchFocusSignal.emit();
                });
            };
            const doc = document as ViewTransitionDocument;
            if (doc.startViewTransition) {
                const transition = doc.startViewTransition(go);
                // Re-issue the focus request once the transition settles as a fallback:
                // the synchronous focus above can be interrupted by the morph, and by now
                // the nav field is fully mounted and interactive.
                if (shouldFocus) {
                    transition.finished.then(searchFocusSignal.emit).catch(() => undefined);
                }
            } else {
                go();
            }
        },
        [search, setQuery, searchFocusSignal.emit]
    );

    const debouncedSearch = useDebouncedCallback(searchWithTransition, SEARCH_DEBOUNCE_MS);

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setLocalQuery(val);
        if (val.trim()) {
            debouncedSearch(val.trim());
        } else {
            // Clearing the field shouldn't navigate away from the home page.
            debouncedSearch.cancel();
        }
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        debouncedSearch.cancel();
        if (query.trim()) searchWithTransition(query.trim());
    };

    return (
        <Section
            component='section'
            sx={{
                pt: { xs: '2.75rem', sm: '4.875rem' },
                pb: { xs: '1.125rem', sm: '1.875rem' },
                textAlign: 'center'
            }}>
            {SearchHeader && <SearchHeader />}
            <Box component='form' onSubmit={handleSubmit} sx={{ maxWidth: '41.25rem', mx: 'auto' }}>
                {/* The nav field claims 'vt-search' while the hero is unregistered — duplicate names abort transitions. */}
                <HeroSearchWrap style={{ viewTransitionName: isActiveSearchBar ? 'vt-search' : undefined }}>
                    <Box
                        component='span'
                        sx={{
                            fontFamily: MONO_FONT,
                            color: 'secondary.light',
                            fontSize: '1.25rem',
                            flexShrink: 0,
                            userSelect: 'none'
                        }}>
                        /
                    </Box>
                    <HeroInput
                        ref={heroInputRef}
                        aria-label='Search extensions'
                        value={query}
                        onChange={handleInputChange}
                        placeholder='search extensions…'
                    />
                    <HeroSubmitButton type='submit' aria-label='Search'>
                        <svg
                            width='16'
                            height='16'
                            viewBox='0 0 24 24'
                            fill='none'
                            stroke='currentColor'
                            strokeWidth='2.4'>
                            <circle cx='11' cy='11' r='7' />
                            <path d='M21 21l-4.3-4.3' />
                        </svg>
                        <Box component='span' sx={{ display: { xs: 'none', sm: 'inline' } }}>
                            search
                        </Box>
                    </HeroSubmitButton>
                </HeroSearchWrap>
            </Box>
            {popularSearches.length > 0 && (
                <Box
                    sx={{
                        display: 'flex',
                        gap: '0.5625rem',
                        justifyContent: { xs: 'flex-start', sm: 'center' },
                        flexWrap: { xs: 'nowrap', sm: 'wrap' },
                        overflowX: { xs: 'auto', sm: 'visible' },
                        mt: '1.125rem',
                        mx: { xs: '-1rem', sm: 0 },
                        px: { xs: '1rem', sm: 0 },
                        pb: { xs: '0.25rem', sm: 0 },
                        '&::-webkit-scrollbar': { display: 'none' },
                        scrollbarWidth: 'none'
                    }}>
                    <Typography
                        component='span'
                        sx={{ fontSize: '0.8125rem', color: 'text.disabled', alignSelf: 'center', flexShrink: 0 }}>
                        Popular:
                    </Typography>
                    {popularSearches.map(chip => (
                        <PopularChip key={chip} onClick={() => searchWithTransition(chip)} style={{ flexShrink: 0 }}>
                            {chip}
                        </PopularChip>
                    ))}
                </Box>
            )}
        </Section>
    );
};
