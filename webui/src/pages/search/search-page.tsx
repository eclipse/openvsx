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

import { FunctionComponent, useState } from 'react';
import { Box } from '@mui/material';
import { SortBy, SortOrder } from '../../extension-registry-types';
import { ExtensionCategory } from '../../extension-registry-types';
import { ExtensionList } from '../../components/extension-list';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryListItem } from '../../components/category-list-item';
import { Eyebrow, Section } from '../../components/page-primitives';
import { NAVBAR_HEIGHT } from '../../default/theme';
import { useSearch } from '../../hooks/use-search';
import { useCategories } from '../../components/categories';
import { SearchHeader } from './search-header';

export const SearchPage: FunctionComponent = () => {
    const { filter, search } = useSearch();
    const { query: searchQuery, category, sortBy, sortOrder } = filter;
    const categories = useCategories();

    const [resultNumber, setResultNumber] = useState(0);

    return (
        <Section
            sx={{
                pb: { xs: '1.125rem', sm: '1.875rem' }
            }}>
            {/* Mobile category pills — outside the flex row so negative-margin bleed isn't clipped */}
            {categories.length > 0 && (
                <Box
                    sx={{
                        display: { xs: 'flex', md: 'none' },
                        flexWrap: 'nowrap',
                        overflowX: 'auto',
                        gap: '0.5rem',
                        mx: { xs: '-1rem', md: 0 },
                        px: { xs: '1rem', md: 0 },
                        pt: '1.25rem',
                        pb: '0.25rem',
                        '&::-webkit-scrollbar': { display: 'none' },
                        scrollbarWidth: 'none'
                    }}>
                    {(['', ...categories] as Array<ExtensionCategory | ''>).map(cat => {
                        const Icon = cat ? CATEGORY_ICONS[cat] : DefaultCategoryIcon;
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
                        pr: '0.75rem',
                        // Breathing room so focus outlines aren't clipped by the scroll container
                        pl: '0.25rem',
                        ml: '-0.25rem',
                        pt: '1.75rem',
                        pb: '2.5rem',
                        display: { xs: 'none', md: 'block' },
                        position: 'sticky',
                        top: NAVBAR_HEIGHT,
                        alignSelf: 'flex-start',
                        maxHeight: `calc(100vh - ${NAVBAR_HEIGHT} - 1.125rem)`,
                        overflowY: 'auto',
                        zIndex: 50
                    }}>
                    <Eyebrow sx={{ mb: '0.625rem', px: '0.625rem' }}>Categories</Eyebrow>
                    {(['', ...categories] as Array<ExtensionCategory | ''>).map(cat => {
                        const Icon = cat ? CATEGORY_ICONS[cat] : DefaultCategoryIcon;
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
                        filter={{ query: searchQuery, category, offset: 0, size: 25, sortBy, sortOrder }}
                        onUpdate={setResultNumber}
                    />
                </Box>
            </Box>
        </Section>
    );
};
