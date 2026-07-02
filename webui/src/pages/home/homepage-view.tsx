/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ComponentType, FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { Box, ButtonBase, Typography } from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { MainContext } from '../../context';
import { useSearchSync } from '../../search-sync-context';
import { ExtensionCard } from '../../components/extension-card';
import { SearchEntry, SearchResult, SortOrder, isError } from '../../extension-registry-types';
import { HomeCuratedSection } from '../../page-settings';
import { MONO_FONT } from '../../default/theme';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryCard } from '../../components/category-card';
import { Section, Eyebrow } from '../../components/layout';

const CURATED_SIZE = 6;
const EXCLUDED_CATEGORIES = new Set(['Other', 'SCM Providers', 'Extension Packs']);

/** Curated rows shown when the consumer does not configure `home.curatedSections`. */
const DEFAULT_CURATED_SECTIONS: HomeCuratedSection[] = [
    { title: 'Most downloaded', subtitle: 'The extensions developers rely on every day', sortBy: 'downloadCount' },
    { title: 'Recently updated', subtitle: 'Fresh releases from publishers this week', sortBy: 'timestamp' }
];

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
    color: '#fff',
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
    '&:hover': {
        borderColor: theme.palette.secondary.main,
        color: theme.palette.secondary.light
    }
}));

const GetInvolvedCard = styled(Box)(({ theme }) => ({
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '16px',
    padding: '24px',
    display: 'flex',
    flexDirection: 'column'
}));

interface CuratedRow extends HomeCuratedSection {
    extensions: SearchEntry[];
    loading: boolean;
}

interface HomepageViewProps {
    onSearch: (query: string, category?: string) => void;
}

interface HeroSearchProps {
    onSearch: HomepageViewProps['onSearch'];
    searchHeader?: ComponentType;
    popularSearches: string[];
}

/**
 * Owns the hero search field's `query` state so keystrokes only re-render the
 * field, not the whole homepage (categories grid and curated rows).
 */
const HeroSearch: FunctionComponent<HeroSearchProps> = ({ onSearch, searchHeader: SearchHeader, popularSearches }) => {
    const { navQuery } = useSearchSync();
    const [query, setQuery] = useState(() => navQuery);
    const searchTimerRef = useRef<number>();

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setQuery(val);
        clearTimeout(searchTimerRef.current);
        if (val.trim()) {
            searchTimerRef.current = window.setTimeout(() => onSearch(val.trim()), 250);
        }
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        clearTimeout(searchTimerRef.current);
        if (query.trim()) onSearch(query.trim());
    };

    return (
        <Box
            component='section'
            sx={{
                maxWidth: '1080px',
                mx: 'auto',
                pt: { xs: '44px', sm: '78px' },
                pb: { xs: '18px', sm: '30px' },
                px: '28px',
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
                        id='hero-search-input'
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
                        mx: { xs: '-28px', sm: 0 },
                        px: { xs: '28px', sm: 0 },
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
                        <PopularChip key={chip} onClick={() => onSearch(chip)} style={{ flexShrink: 0 }}>
                            {chip}
                        </PopularChip>
                    ))}
                </Box>
            )}
        </Box>
    );
};

export const HomepageView: FunctionComponent<HomepageViewProps> = ({ onSearch }) => {
    const { service, pageSettings } = useContext(MainContext);
    const SearchHeader = pageSettings.elements.searchHeader;
    const home = pageSettings.elements.home;
    const popularSearches = home?.popularSearches ?? [];
    const curatedSections = home?.curatedSections ?? DEFAULT_CURATED_SECTIONS;
    const involvement = home?.involvement;

    const [categories, setCategories] = useState<string[]>([]);
    const [rows, setRows] = useState<CuratedRow[]>(() =>
        curatedSections.map(section => ({ ...section, extensions: [], loading: true }))
    );

    useEffect(() => {
        setCategories(
            Array.from(service.getCategories())
                .filter(c => !EXCLUDED_CATEGORIES.has(c))
                .sort((a, b) => a.localeCompare(b))
        );

        const abortController = new AbortController();
        curatedSections.forEach((section, idx) => {
            service
                .search(abortController, {
                    query: '',
                    category: '',
                    offset: 0,
                    size: CURATED_SIZE,
                    sortBy: section.sortBy,
                    sortOrder: 'desc' as SortOrder
                })
                .then(result => {
                    if (isError(result)) {
                        return;
                    }
                    const { extensions } = result as SearchResult;
                    setRows(prev => prev.map((row, i) => (i === idx ? { ...row, extensions, loading: false } : row)));
                })
                .catch(() => setRows(prev => prev.map((row, i) => (i === idx ? { ...row, loading: false } : row))));
        });
        return () => abortController.abort();
    }, [service, curatedSections]);

    return (
        <Box component='main' sx={{ animation: 'fadeIn .25s ease' }}>
            <HeroSearch onSearch={onSearch} searchHeader={SearchHeader} popularSearches={popularSearches} />

            {categories.length > 0 && (
                <Section component='section' sx={{ mt: { xs: '22px', sm: '36px' } }}>
                    <Eyebrow sx={{ mb: { xs: '12px', sm: '18px' } }}>Browse by category</Eyebrow>
                    <Box
                        sx={{
                            display: { xs: 'flex', sm: 'none' },
                            flexWrap: 'nowrap',
                            overflowX: 'auto',
                            gap: '8px',
                            mx: '-16px',
                            px: '16px',
                            pb: '4px',
                            '&::-webkit-scrollbar': { display: 'none' },
                            scrollbarWidth: 'none'
                        }}>
                        {categories.map(cat => (
                            <CategoryPill
                                key={cat}
                                label={cat}
                                icon={CATEGORY_ICONS[cat] ?? DefaultCategoryIcon}
                                onClick={() => onSearch('', cat)}
                            />
                        ))}
                    </Box>
                    <Box
                        sx={{
                            display: { xs: 'none', sm: 'grid' },
                            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                            gap: '14px'
                        }}>
                        {categories.map(cat => (
                            <CategoryCard
                                key={cat}
                                label={cat}
                                icon={CATEGORY_ICONS[cat] ?? DefaultCategoryIcon}
                                onClick={() => onSearch('', cat)}
                            />
                        ))}
                    </Box>
                </Section>
            )}

            {rows.map(
                row =>
                    !row.loading &&
                    row.extensions.length > 0 && (
                        <Section component='section' key={row.title} sx={{ mt: { xs: '36px', sm: '54px' } }}>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    mb: '18px'
                                }}>
                                <Box>
                                    <Typography
                                        sx={{
                                            fontSize: { xs: '16px', sm: '23px' },
                                            fontWeight: 700,
                                            letterSpacing: '-0.02em'
                                        }}>
                                        {row.title}
                                    </Typography>
                                    <Typography
                                        component='span'
                                        sx={{
                                            fontSize: '13.5px',
                                            color: 'text.disabled',
                                            display: { xs: 'none', sm: 'block' }
                                        }}>
                                        {row.subtitle}
                                    </Typography>
                                </Box>
                                <Box
                                    component='button'
                                    onClick={() => onSearch('')}
                                    sx={{
                                        background: 'none',
                                        border: 'none',
                                        color: 'secondary.light',
                                        fontSize: '14px',
                                        fontWeight: 600,
                                        cursor: 'pointer'
                                    }}>
                                    See all →
                                </Box>
                            </Box>
                            <Box
                                sx={{
                                    display: 'grid',
                                    gridTemplateColumns: {
                                        xs: 'repeat(2, 1fr)',
                                        sm: 'repeat(auto-fill, minmax(190px, 1fr))'
                                    },
                                    gap: '16px'
                                }}>
                                {row.extensions.map((ext, idx) => (
                                    <ExtensionCard
                                        key={`${ext.namespace}.${ext.name}`}
                                        extension={ext}
                                        idx={idx}
                                        filterSize={CURATED_SIZE}
                                    />
                                ))}
                            </Box>
                        </Section>
                    )
            )}

            {involvement && involvement.cards.length > 0 && (
                <Section component='section' sx={{ mt: { xs: '48px', sm: '72px' }, mb: { xs: '40px', sm: '56px' } }}>
                    <Eyebrow sx={{ letterSpacing: '0.1em', mb: { xs: '14px', sm: '20px' } }}>
                        {involvement.heading ?? 'Get Involved'}
                    </Eyebrow>
                    <Box
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                            gap: '16px'
                        }}>
                        {involvement.cards.map(card => (
                            <GetInvolvedCard key={card.title}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: '11px', mb: '12px' }}>
                                    <Box
                                        sx={{
                                            width: '34px',
                                            height: '34px',
                                            borderRadius: '9px',
                                            bgcolor: 'accentSoft',
                                            color: 'secondary.light',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            flexShrink: 0,
                                            '& > svg': { fontSize: 18 }
                                        }}>
                                        {card.icon}
                                    </Box>
                                    <Typography sx={{ fontSize: '15.5px', fontWeight: 700 }}>{card.title}</Typography>
                                </Box>
                                <Typography
                                    sx={{
                                        fontSize: '13.5px',
                                        color: 'text.secondary',
                                        lineHeight: 1.55,
                                        mb: '18px',
                                        flex: 1
                                    }}>
                                    {card.description}
                                </Typography>
                                <Box
                                    component='a'
                                    href={card.href}
                                    target='_blank'
                                    sx={{
                                        fontSize: '13.5px',
                                        fontWeight: 600,
                                        color: 'secondary.light',
                                        textDecoration: 'none',
                                        '&:hover': { textDecoration: 'underline' }
                                    }}>
                                    {card.label}
                                </Box>
                            </GetInvolvedCard>
                        ))}
                    </Box>
                </Section>
            )}
        </Box>
    );
};
