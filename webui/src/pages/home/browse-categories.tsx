/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { ExtensionCategory } from '../../extension-registry-types';
import { CATEGORY_ICONS, DefaultCategoryIcon } from '../../components/categories';
import { CategoryPill } from '../../components/category-pill';
import { CategoryCard } from '../../components/category-card';
import { Section, Eyebrow } from '../../components/layout';

interface BrowseCategoriesProps {
    categories: ExtensionCategory[];
    onSelect: (category: ExtensionCategory) => void;
}

/** "Browse by category" section: a horizontal pill row on mobile, a card grid on desktop. */
export const BrowseCategories: FunctionComponent<BrowseCategoriesProps> = ({ categories, onSelect }) => {
    if (categories.length === 0) {
        return null;
    }
    return (
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
                        onClick={() => onSelect(cat)}
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
                        onClick={() => onSelect(cat)}
                    />
                ))}
            </Box>
        </Section>
    );
};
