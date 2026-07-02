/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

export const CATEGORIES = [
    'AI',
    'Programming Languages',
    'Snippets',
    'Linters',
    'Themes',
    'Debuggers',
    'Formatters',
    'Keymaps',
    'SCM Providers',
    'Other',
    'Extension Packs',
    'Language Packs',
    'Data Science',
    'Machine Learning',
    'Visualization',
    'Notebooks'
] as const;

export type ExtensionCategory = (typeof CATEGORIES)[number];

const SORTED_CATEGORIES: ExtensionCategory[] = [...CATEGORIES].sort((a, b) => {
    if (a === 'Other') return 1;
    if (b === 'Other') return -1;
    return a.localeCompare(b);
});

export function useCategories(): ExtensionCategory[] {
    return SORTED_CATEGORIES;
}
