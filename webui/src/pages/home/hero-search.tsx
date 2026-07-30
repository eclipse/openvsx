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

import { ChangeEvent, ComponentType, FormEvent, FunctionComponent, useCallback, useLayoutEffect, useRef } from 'react';
import { Box, ButtonBase, Container, Typography } from '@mui/material';
import { useLocation } from 'react-router';
import { styled, alpha } from '@mui/material/styles';
import { accentHover, focusOutline, focusRing } from '../../components/page-primitives';
import { useSearch } from '../../hooks/use-search';
import { useSearchQuery } from '../../context/search/search-context';
import { useSearchFocus } from '../../context/search/search-focus-context';
import { useRegisterPageSearchBar } from '../../context/search/page-search-bar-context';
import { useSignalEffect } from '../../hooks/use-signal-effect';
import { MONO_FONT } from '../../default/theme';

const HeroSearchWrap = styled(Box)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.8125rem',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: theme.shape.borderRadiusCard,
    height: '3.25rem',
    paddingLeft: '1rem',
    paddingRight: '0.25rem',
    [theme.breakpoints.down('sm')]: {
        height: '3rem',
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
    fontSize: '0.9375rem',
    fontFamily: MONO_FONT,
    '&::placeholder': { color: theme.palette.text.disabled }
}));

const HeroSubmitButton = styled(ButtonBase)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    height: '2.75rem',
    padding: '0 1rem',
    borderRadius: theme.shape.borderRadius,
    overflow: 'hidden',
    backgroundColor: theme.palette.secondary.main,
    color: theme.palette.secondary.contrastText,
    fontSize: '0.8125rem',
    fontWeight: 400,
    flexShrink: 0,
    transition: 'background 0.14s',
    [theme.breakpoints.down('sm')]: {
        height: '2.5rem',
        padding: '0 0.875rem'
    },
    '&:hover': { backgroundColor: theme.palette.secondary.dark },
    ...focusOutline(theme)
}));

const PopularChip = styled(ButtonBase)(({ theme }) => ({
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    color: theme.palette.text.secondary,
    fontSize: '0.8125rem',
    fontWeight: 400,
    padding: '0.3125rem 0.625rem',
    borderRadius: theme.shape.borderRadiusPill,
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
 * view (the nav bar hides its own field) and binds to the shared draft query.
 */
export const HeroSearch: FunctionComponent<HeroSearchProps> = ({
    searchHeader: SearchHeader,
    popularSearches = []
}) => {
    const { query } = useSearchQuery();
    const { search } = useSearch();
    const { searchFocusSignal, searchFocused } = useSearchFocus();
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

    // Route focus across the scroll swap: the field taking over grabs focus if
    // its counterpart had it (the signal routes to the active bar).
    const wasActiveSearchBar = useRef(isActiveSearchBar);
    useLayoutEffect(() => {
        if (wasActiveSearchBar.current === isActiveSearchBar) {
            return;
        }
        wasActiveSearchBar.current = isActiveSearchBar;
        if (isActiveSearchBar) {
            if (searchFocused) searchFocusSignal.emit();
        } else if (document.activeElement === heroInputRef.current) {
            searchFocusSignal.emit();
        }
    }, [isActiveSearchBar, searchFocused, searchFocusSignal.emit]);

    // Hand focus to the nav field when the hero unmounts mid-typing — otherwise
    // focus falls to <body> until the morph ends and keystrokes are swallowed.
    useLayoutEffect(
        () => () => {
            if (document.activeElement === heroInputRef.current) {
                searchFocusSignal.emit();
            }
        },
        [searchFocusSignal.emit]
    );

    // Focus the search by default when the hero page is the app's landing page.
    const { key: locationKey } = useLocation();
    useLayoutEffect(() => {
        if (locationKey === 'default') {
            searchFocusSignal.emit();
        }
    }, [locationKey, searchFocusSignal.emit]);

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) =>
        search({ query: e.target.value }, { debounce: true });

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (query.trim()) search({ query });
    };

    return (
        <Container maxWidth='xl' component='section' sx={{ textAlign: 'center' }}>
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
                        mt: '0.875rem',
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
                        <PopularChip key={chip} onClick={() => search({ query: chip })} style={{ flexShrink: 0 }}>
                            {chip}
                        </PopularChip>
                    ))}
                </Box>
            )}
        </Container>
    );
};
