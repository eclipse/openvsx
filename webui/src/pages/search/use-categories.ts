/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { CATEGORIES, ExtensionCategory } from '../../extension-registry-types';

const SORTED_CATEGORIES: ExtensionCategory[] = [...CATEGORIES].sort((a, b) => {
    if (a === 'Other') return 1;
    if (b === 'Other') return -1;
    return a.localeCompare(b);
});

export function useCategories(): ExtensionCategory[] {
    return SORTED_CATEGORIES;
}
