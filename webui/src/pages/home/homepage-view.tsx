/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useEffect, useRef, useState } from 'react';
import { Box, ButtonBase, Typography } from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { MainContext } from '../../context';
import { useNavSearch } from '../../nav-search-context';
import { ExtensionCard } from '../../components/extension-card';
import { SearchEntry, isError } from '../../extension-registry-types';
import { SortBy, SortOrder } from '../../extension-registry-types';
import { MONO_FONT } from '../../default/theme';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryCard } from '../../components/category-card';

const POPULAR_CHIPS = ['python', 'git', 'docker', 'prettier', 'eslint', 'rust', 'java'];

// ---- Styled components ----

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

// ---- Types ----

interface CuratedSection {
    title: string;
    sub: string;
    sortBy: SortBy;
    extensions: SearchEntry[];
    loading: boolean;
}

interface HomepageViewProps {
    onSearch: (query: string, category?: string) => void;
}

// ---- Component ----

export const HomepageView: FunctionComponent<HomepageViewProps> = ({ onSearch }) => {
    const context = useContext(MainContext);
    const { navQuery } = useNavSearch();
    const [query, setQuery] = useState(() => navQuery);
    const [categories, setCategories] = useState<string[]>([]);
    const [sections, setSections] = useState<CuratedSection[]>([
        {
            title: 'Most Downloaded',
            sub: 'The extensions developers rely on every day',
            sortBy: 'downloadCount',
            extensions: [],
            loading: true
        },
        {
            title: 'Recently Updated',
            sub: 'Fresh releases from publishers this week',
            sortBy: 'timestamp',
            extensions: [],
            loading: true
        }
    ]);
    const abortRef = useRef(new AbortController());
    const searchTimerRef = useRef<number>();

    useEffect(() => {
        const EXCLUDED = new Set(['Other', 'SCM Providers', 'Extension Packs']);
        const cats = Array.from(context.service.getCategories())
            .filter(c => !EXCLUDED.has(c))
            .sort((a, b) => a.localeCompare(b));
        setCategories(cats);

        const ac = abortRef.current;
        sections.forEach((section, idx) => {
            context.service
                .search(ac, {
                    query: '',
                    category: '',
                    offset: 0,
                    size: 6,
                    sortBy: section.sortBy,
                    sortOrder: 'desc' as SortOrder
                })
                .then(result => {
                    if (!isError(result)) {
                        setSections(prev =>
                            prev.map((s, i) =>
                                i === idx ? { ...s, extensions: (result as any).extensions ?? [], loading: false } : s
                            )
                        );
                    }
                })
                .catch(() => setSections(prev => prev.map((s, i) => (i === idx ? { ...s, loading: false } : s))));
        });

        return () => ac.abort();
    }, []);

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
        <Box component='main' sx={{ animation: 'fadeIn .25s ease' }}>
            {/* ---- Hero ---- */}
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
                <Box
                    sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '8px',
                        px: '13px',
                        py: '6px',
                        borderRadius: '999px',
                        bgcolor: 'accentSoft',
                        color: 'secondary.light',
                        fontSize: '12.5px',
                        fontWeight: 600,
                        mb: { xs: 2, sm: 3 }
                    }}>
                    <Box
                        component='span'
                        sx={{
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            bgcolor: 'secondary.main',
                            display: 'inline-block',
                            flexShrink: 0
                        }}
                    />
                    Open-source registry for VS Code–compatible editors
                </Box>
                <Typography
                    component='h1'
                    sx={{
                        fontSize: { xs: '2.2rem', sm: '3rem', md: '3.375rem' },
                        lineHeight: 1.04,
                        letterSpacing: '-0.035em',
                        fontWeight: 800,
                        mb: { xs: '12px', sm: '18px' }
                    }}>
                    Find the right extension,
                    <br />
                    for any editor.
                </Typography>
                <Typography
                    sx={{
                        fontSize: { xs: '15px', sm: '18px' },
                        color: 'text.secondary',
                        maxWidth: '560px',
                        mx: 'auto',
                        mb: { xs: '22px', sm: '36px' },
                        lineHeight: 1.5
                    }}>
                    Browse community-published extensions. Free, open, and vendor-neutral.
                </Typography>
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
                    {POPULAR_CHIPS.map(chip => (
                        <PopularChip key={chip} onClick={() => onSearch(chip)} style={{ flexShrink: 0 }}>
                            {chip}
                        </PopularChip>
                    ))}
                </Box>
            </Box>

            {/* ---- Categories ---- */}
            {categories.length > 0 && (
                <Box
                    component='section'
                    sx={{ maxWidth: '1320px', mx: 'auto', mt: { xs: '22px', sm: '36px' }, px: '28px' }}>
                    <Typography
                        sx={{
                            fontSize: { xs: '12px', sm: '14px' },
                            fontWeight: 600,
                            color: 'text.disabled',
                            textTransform: 'uppercase',
                            letterSpacing: '0.07em',
                            mb: { xs: '12px', sm: '18px' }
                        }}>
                        Browse by category
                    </Typography>
                    {/* Mobile: horizontal scrollable pills */}
                    <Box
                        sx={{
                            display: { xs: 'flex', sm: 'none' },
                            flexWrap: 'nowrap',
                            overflowX: 'auto',
                            gap: '8px',
                            mx: '-28px',
                            px: '28px',
                            pb: '4px',
                            '&::-webkit-scrollbar': { display: 'none' },
                            scrollbarWidth: 'none'
                        }}>
                        {categories.map(cat => {
                            const Icon = CATEGORY_ICONS[cat] ?? DefaultCategoryIcon;
                            return <CategoryPill key={cat} label={cat} icon={Icon} onClick={() => onSearch('', cat)} />;
                        })}
                    </Box>
                    {/* Desktop: grid */}
                    <Box
                        sx={{
                            display: { xs: 'none', sm: 'grid' },
                            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                            gap: '14px'
                        }}>
                        {categories.map(cat => {
                            const Icon = CATEGORY_ICONS[cat] ?? DefaultCategoryIcon;
                            return <CategoryCard key={cat} label={cat} icon={Icon} onClick={() => onSearch('', cat)} />;
                        })}
                    </Box>
                </Box>
            )}

            {/* ---- Curated rows ---- */}
            {sections.map(
                section =>
                    !section.loading &&
                    section.extensions.length > 0 && (
                        <Box
                            component='section'
                            key={section.title}
                            sx={{ maxWidth: '1320px', mx: 'auto', mt: { xs: '36px', sm: '54px' }, px: '28px' }}>
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
                                        {section.title}
                                    </Typography>
                                    <Typography
                                        component='span'
                                        sx={{
                                            fontSize: '13.5px',
                                            color: 'text.disabled',
                                            display: { xs: 'none', sm: 'block' }
                                        }}>
                                        {section.sub}
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
                                {section.extensions.map((ext, idx) => (
                                    <ExtensionCard
                                        key={`${ext.namespace}.${ext.name}`}
                                        extension={ext}
                                        idx={idx}
                                        filterSize={6}
                                    />
                                ))}
                            </Box>
                        </Box>
                    )
            )}

            {/* ---- Get Involved ---- */}
            <Box
                component='section'
                sx={{
                    maxWidth: '1320px',
                    mx: 'auto',
                    mt: { xs: '48px', sm: '72px' },
                    mb: { xs: '40px', sm: '56px' },
                    px: '28px'
                }}>
                <Typography
                    sx={{
                        fontSize: { xs: '11px', sm: '12px' },
                        fontWeight: 700,
                        textTransform: 'uppercase',
                        letterSpacing: '0.1em',
                        color: 'text.disabled',
                        mb: { xs: '14px', sm: '20px' }
                    }}>
                    Get Involved
                </Typography>
                <Box
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                        gap: '16px'
                    }}>
                    {GET_INVOLVED_CARDS.map(card => (
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
                                        flexShrink: 0
                                    }}>
                                    <GetInvolvedIcon icon={card.icon} />
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
                                {card.desc}
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
            </Box>
        </Box>
    );
};

const GET_INVOLVED_CARDS = [
    {
        icon: 'fork',
        title: 'Contribute',
        desc: 'Open VSX is fully open source. Help build the registry the ecosystem depends on.',
        href: 'https://github.com/eclipse/openvsx',
        label: 'View on GitHub →'
    },
    {
        icon: 'group',
        title: 'Join the Working Group',
        desc: 'Shape the future of an open, vendor-neutral marketplace for extensions.',
        href: 'https://openvsxworkinggroup.github.io/',
        label: 'Learn more →'
    },
    {
        icon: 'book',
        title: 'Read the docs',
        desc: 'Learn how to publish, claim namespaces, and consume extensions via the API.',
        href: 'https://github.com/eclipse/openvsx/wiki',
        label: 'Open documentation →'
    }
];

const GetInvolvedIcon: FunctionComponent<{ icon: string }> = ({ icon }) => {
    if (icon === 'fork')
        return (
            <svg
                width='18'
                height='18'
                viewBox='0 0 24 24'
                fill='none'
                stroke='currentColor'
                strokeWidth='2'
                strokeLinecap='round'
                strokeLinejoin='round'>
                <line x1='6' x2='6' y1='3' y2='15' />
                <circle cx='18' cy='6' r='3' />
                <circle cx='6' cy='18' r='3' />
                <path d='M18 9a9 9 0 0 1-9 9' />
            </svg>
        );
    if (icon === 'group')
        return (
            <svg
                width='18'
                height='18'
                viewBox='0 0 24 24'
                fill='none'
                stroke='currentColor'
                strokeWidth='2'
                strokeLinecap='round'
                strokeLinejoin='round'>
                <path d='M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' />
                <circle cx='9' cy='7' r='4' />
                <path d='M22 21v-2a4 4 0 0 0-3-3.87' />
                <path d='M16 3.13a4 4 0 0 1 0 7.75' />
            </svg>
        );
    return (
        <svg
            width='18'
            height='18'
            viewBox='0 0 24 24'
            fill='none'
            stroke='currentColor'
            strokeWidth='2'
            strokeLinecap='round'
            strokeLinejoin='round'>
            <path d='M12 7v14' />
            <path d='M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z' />
        </svg>
    );
};
