/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useLayoutEffect, useState } from 'react';
import { Box, Container } from '@mui/material';
import { SortBy, SortOrder } from '../../extension-registry-types';
import { ExtensionCategory } from './use-categories';
import { ExtensionList } from '../../components/extension-list';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryListItem } from '../../components/category-list-item';
import { Eyebrow } from '../../components/layout';
import { useSearch } from '../../hooks/use-search';
import { useCategories } from './use-categories';
import { SearchHeader } from './search-header';

export const SearchPage: FunctionComponent = () => {
    const { filter, search } = useSearch();
    const { query: searchQuery, category, sortBy, sortOrder } = filter;
    const categories = useCategories();

    const [resultNumber, setResultNumber] = useState(0);

    useLayoutEffect(() => {
        // Entering the results should always start at the top, not wherever the
        // hero/home page happened to be scrolled when the search was triggered.
        window.scrollTo({ top: 0, left: 0 });
    }, []);

    return (
        <Container
            sx={{
                pb: { xs: '18px', sm: '30px' }
            }}
            maxWidth='xl'>
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
                                onClick={() => search({ category: cat })}
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
                        top: '60px',
                        alignSelf: 'flex-start',
                        maxHeight: 'calc(100vh - 80px)',
                        overflowY: 'auto',
                        zIndex: 50
                    }}>
                    <Eyebrow sx={{ mb: '10px', px: '10px' }}>Categories</Eyebrow>
                    {(['', ...categories] as Array<ExtensionCategory | ''>).map(cat => {
                        const Icon = CATEGORY_ICONS[cat] ?? DefaultCategoryIcon;
                        return (
                            <CategoryListItem
                                key={cat || '_all'}
                                label={cat || 'All categories'}
                                icon={Icon}
                                isSelected={category === cat}
                                onClick={() => search({ category: cat })}
                            />
                        );
                    })}
                </Box>

                {/* Main content */}
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <SearchHeader
                        resultNumber={resultNumber}
                        sortBy={sortBy}
                        sortOrder={sortOrder}
                        searchQuery={searchQuery}
                        category={category}
                        onSortByChanged={(sortBy: SortBy) => search({ sortBy })}
                        onSortOrderChanged={(sortOrder: SortOrder) => search({ sortOrder })}
                    />
                    <ExtensionList
                        filter={{ query: searchQuery, category, offset: 0, size: 10, sortBy, sortOrder }}
                        onUpdate={setResultNumber}
                    />
                </Box>
            </Box>
        </Container>
    );
};
