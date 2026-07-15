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

import { FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { ExtensionCategory } from '../../extension-registry-types';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryCard } from '../../components/category-card';
import { Section, Eyebrow } from '../../components/page-primitives';
import { useSearch } from '../../hooks/use-search';
import { useHomeCategories } from './use-home-data';

export interface BrowseCategoriesProps {
    /** Categories to offer; defaults to the registry's browsable home list. */
    categories?: ExtensionCategory[];
    /** Category click handler; defaults to opening the search page filtered to it. */
    onSelect?: (category: ExtensionCategory) => void;
}

/** "Browse by category" section: a horizontal pill row on mobile, a card grid on desktop. */
export const BrowseCategories: FunctionComponent<BrowseCategoriesProps> = props => {
    const homeCategories = useHomeCategories();
    const { search } = useSearch();
    const categories = props.categories ?? homeCategories;
    const onSelect = props.onSelect ?? ((category: ExtensionCategory) => search({ query: '', category }));
    if (categories.length === 0) {
        return null;
    }
    return (
        <Section component='section' sx={{ mt: { xs: '1.375rem', sm: '2.25rem' } }}>
            <Eyebrow sx={{ mb: { xs: '0.75rem', sm: '1.125rem' } }}>Browse by category</Eyebrow>
            <Box
                sx={{
                    display: { xs: 'flex', sm: 'none' },
                    flexWrap: 'nowrap',
                    overflowX: 'auto',
                    gap: '0.5rem',
                    mx: '-1rem',
                    px: '1rem',
                    pb: '0.25rem',
                    '&::-webkit-scrollbar': { display: 'none' },
                    scrollbarWidth: 'none'
                }}>
                {categories.map(cat => (
                    <CategoryPill
                        key={cat}
                        label={cat}
                        icon={CATEGORY_ICONS[cat] ?? DefaultCategoryIcon}
                        onClick={() => onSelect(cat)}
                    />
                ))}
            </Box>
            <Box
                sx={{
                    display: { xs: 'none', sm: 'flex' },
                    flexWrap: 'wrap',
                    justifyContent: 'center',
                    gap: '0.625rem'
                }}>
                {categories.map(cat => (
                    <CategoryCard
                        key={cat}
                        label={cat}
                        icon={CATEGORY_ICONS[cat] ?? DefaultCategoryIcon}
                        onClick={() => onSelect(cat)}
                    />
                ))}
            </Box>
        </Section>
    );
};
