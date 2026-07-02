/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ChangeEvent, ComponentType, FormEvent, FunctionComponent, useCallback, useRef, useState } from 'react';
import { Box, ButtonBase, Container, Typography } from '@mui/material';
import { flushSync } from 'react-dom';
import { styled, alpha } from '@mui/material/styles';
import { accentHover } from '../../components/layout';
import { useSearch } from '../../hooks/use-search';
import { useSearchFocus } from '../../hooks/use-search-focus';
import { useSignalEffect } from '../../hooks/use-signal-effect';
import { useDebouncedCallback } from '../../hooks/use-debounced-callback';
import { MONO_FONT } from '../../default/theme';

const HeroSearchWrap = styled(Box)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '13px',
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '15px',
    height: '62px',
    paddingLeft: '20px',
    paddingRight: '8px',
    [theme.breakpoints.down('sm')]: {
        height: '54px',
        paddingLeft: '14px',
        gap: '10px'
    },
    boxShadow: 'var(--shadow)',
    transition: 'border-color 0.2s ease, box-shadow 0.3s ease',
    '&:focus-within': {
        borderColor: theme.palette.secondary.main,
        boxShadow: `0 0 0 3px ${alpha(theme.palette.secondary.main, 0.16)}, 0 18px 70px -10px ${alpha(theme.palette.secondary.main, 0.45)}`
    }
}));

const HeroInput = styled('input')(({ theme }) => ({
    flex: 1,
    height: '100%',
    border: 'none',
    outline: 'none',
    background: 'none',
    color: theme.palette.text.primary,
    fontSize: '17px',
    fontFamily: MONO_FONT,
    '&::placeholder': { color: theme.palette.text.disabled }
}));

const HeroSubmitButton = styled(ButtonBase)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    height: '46px',
    padding: '0 22px',
    borderRadius: '11px',
    overflow: 'hidden',
    backgroundColor: theme.palette.secondary.main,
    color: theme.palette.secondary.contrastText,
    fontSize: '15px',
    fontWeight: 600,
    flexShrink: 0,
    transition: 'background 0.14s',
    [theme.breakpoints.down('sm')]: {
        height: '40px',
        padding: '0 14px',
        borderRadius: '9px'
    },
    '&:hover': { backgroundColor: theme.palette.secondary.dark }
}));

const PopularChip = styled(ButtonBase)(({ theme }) => ({
    backgroundColor: theme.palette.surface2,
    border: `1px solid ${theme.palette.divider}`,
    color: theme.palette.text.secondary,
    fontSize: '13px',
    fontWeight: 500,
    padding: '6px 13px',
    borderRadius: '999px',
    overflow: 'hidden',
    fontFamily: MONO_FONT,
    transition: 'border-color 0.14s, color 0.14s',
    ...accentHover(theme)
}));

interface HeroSearchProps {
    searchHeader?: ComponentType;
    popularSearches: string[];
}

/**
 * Owns a local copy of the hero field's `query` so keystrokes only re-render the
 * field, not the whole homepage. Typing debounces `search`; submit and popular
 * chips search immediately.
 */
export const HeroSearch: FunctionComponent<HeroSearchProps> = ({ searchHeader: SearchHeader, popularSearches }) => {
    const { query: initialQuery, setQuery, search } = useSearch();
    const { focusSearchSignal, focusSearch } = useSearchFocus();
    const [query, setLocalQuery] = useState(() => initialQuery);
    const heroInputRef = useRef<HTMLInputElement>(null);

    // Focus the hero input when focus is requested (e.g. the '/' shortcut on the home page).
    useSignalEffect(
        focusSearchSignal,
        useCallback(() => {
            const el = heroInputRef.current;
            if (!el) {
                return;
            }
            el.focus();
            // Move cursor to end so the user can keep typing.
            requestAnimationFrame(() => el.setSelectionRange(el.value.length, el.value.length));
        }, [])
    );

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
                    if (shouldFocus) focusSearch();
                });
            };
            const doc = document as ViewTransitionDocument;
            if (doc.startViewTransition) {
                const transition = doc.startViewTransition(go);
                // Re-issue the focus request once the transition settles as a fallback:
                // the synchronous focus above can be interrupted by the morph, and by now
                // the nav field is fully mounted and interactive.
                if (shouldFocus) {
                    transition.finished.then(focusSearch).catch(() => undefined);
                }
            } else {
                go();
            }
        },
        [search, setQuery, focusSearch]
    );

    const debouncedSearch = useDebouncedCallback(searchWithTransition);

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
        <Container
            component='section'
            sx={{
                pt: { xs: '44px', sm: '78px' },
                pb: { xs: '18px', sm: '30px' },
                textAlign: 'center'
            }}>
            {SearchHeader && <SearchHeader />}
            <Box component='form' onSubmit={handleSubmit} sx={{ maxWidth: '660px', mx: 'auto' }}>
                <HeroSearchWrap style={{ viewTransitionName: 'vt-search' }}>
                    <Box
                        component='span'
                        sx={{
                            fontFamily: MONO_FONT,
                            color: 'secondary.light',
                            fontSize: '20px',
                            flexShrink: 0,
                            userSelect: 'none'
                        }}>
                        /
                    </Box>
                    <HeroInput
                        ref={heroInputRef}
                        value={query}
                        onChange={handleInputChange}
                        placeholder='search extensions…'
                    />
                    <HeroSubmitButton type='submit'>
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
                        gap: '9px',
                        justifyContent: { xs: 'flex-start', sm: 'center' },
                        flexWrap: { xs: 'nowrap', sm: 'wrap' },
                        overflowX: { xs: 'auto', sm: 'visible' },
                        mt: '18px',
                        mx: { xs: '-16px', sm: 0 },
                        px: { xs: '16px', sm: 0 },
                        pb: { xs: '4px', sm: 0 },
                        '&::-webkit-scrollbar': { display: 'none' },
                        scrollbarWidth: 'none'
                    }}>
                    <Typography
                        component='span'
                        sx={{ fontSize: '13px', color: 'text.disabled', alignSelf: 'center', flexShrink: 0 }}>
                        Popular:
                    </Typography>
                    {popularSearches.map(chip => (
                        <PopularChip key={chip} onClick={() => searchWithTransition(chip)} style={{ flexShrink: 0 }}>
                            {chip}
                        </PopularChip>
                    ))}
                </Box>
            )}
        </Container>
    );
};
