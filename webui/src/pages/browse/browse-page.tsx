/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useLayoutEffect } from 'react';
import { Box, Typography } from '@mui/material';
import { ExtensionCategory } from '../../extension-registry-types';
import { useNavSearch } from '../../nav-search-context';
import { ExtensionList } from '../../components/extension-list';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryListItem } from '../../components/category-list-item';
import { useBrowseFilter } from './use-browse-filter';
import { BrowseHeader } from './browse-header';

export const BrowsePage: FunctionComponent = () => {
    const { setIsHeroPage } = useNavSearch();
    const {
        searchQuery,
        category,
        sortBy,
        sortOrder,
        debounceTime,
        resultNumber,
        categories,
        onResultCount,
        onCategoryChanged,
        onSortByChanged,
        onSortOrderChanged
    } = useBrowseFilter();

    useLayoutEffect(() => {
        setIsHeroPage(false);
        return () => setIsHeroPage(false);
    }, []);

    return (
        <Box
            sx={{
                maxWidth: '1320px',
                mx: 'auto',
                px: { xs: '16px', md: '28px' },
                pb: '64px',
                animation: 'fadeIn .2s ease'
            }}>
            {/* Mobile category pills — outside the flex row so negative-margin bleed isn't clipped */}
            {categories.length > 0 && (
                <Box
                    sx={{
                        display: { xs: 'flex', md: 'none' },
                        flexWrap: 'nowrap',
                        overflowX: 'auto',
                        gap: '8px',
                        mx: { xs: '-16px', md: 0 },
                        px: { xs: '16px', md: 0 },
                        pt: '20px',
                        pb: '4px',
                        '&::-webkit-scrollbar': { display: 'none' },
                        scrollbarWidth: 'none'
                    }}>
                    {(['', ...categories] as Array<ExtensionCategory | ''>).map(cat => {
                        const Icon = CATEGORY_ICONS[cat] ?? DefaultCategoryIcon;
                        return (
                            <CategoryPill
                                key={cat || '_all'}
                                label={cat || 'All'}
                                icon={Icon}
                                isSelected={category === cat}
                                onClick={() => onCategoryChanged(cat)}
                            />
                        );
                    })}
                </Box>
            )}

            <Box sx={{ display: 'flex' }}>
                {/* Desktop categories sidebar */}
                <Box
                    component='nav'
                    aria-label='Categories'
                    sx={{
                        width: 210,
                        flexShrink: 0,
                        pr: '12px',
                        pt: '28px',
                        pb: '40px',
                        display: { xs: 'none', md: 'block' },
                        position: 'sticky',
                        top: '72px',
                        alignSelf: 'flex-start',
                        maxHeight: 'calc(100vh - 80px)',
                        overflowY: 'auto'
                    }}>
                    <Typography
                        sx={{
                            fontSize: '11px',
                            fontWeight: 700,
                            textTransform: 'uppercase',
                            letterSpacing: '0.09em',
                            color: 'text.disabled',
                            mb: '10px',
                            px: '10px'
                        }}>
                        Categories
                    </Typography>
                    {(['', ...categories] as Array<ExtensionCategory | ''>).map(cat => {
                        const Icon = CATEGORY_ICONS[cat] ?? DefaultCategoryIcon;
                        return (
                            <CategoryListItem
                                key={cat || '_all'}
                                label={cat || 'All categories'}
                                icon={Icon}
                                isSelected={category === cat}
                                onClick={() => onCategoryChanged(cat)}
                            />
                        );
                    })}
                </Box>

                {/* Main content */}
                <Box sx={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
                    <BrowseHeader
                        resultNumber={resultNumber}
                        sortBy={sortBy}
                        sortOrder={sortOrder}
                        searchQuery={searchQuery}
                        category={category}
                        onSortByChanged={onSortByChanged}
                        onSortOrderChanged={onSortOrderChanged}
                    />
                    <ExtensionList
                        filter={{ query: searchQuery, category, offset: 0, size: 10, sortBy, sortOrder }}
                        debounceTime={debounceTime}
                        onUpdate={onResultCount}
                    />
                </Box>
            </Box>
        </Box>
    );
};
