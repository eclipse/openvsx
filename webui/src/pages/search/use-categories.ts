/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useContext, useEffect, useState } from 'react';
import { MainContext } from '../../context';
import { ExtensionCategory } from '../../extension-registry-types';

export function useCategories(): ExtensionCategory[] {
    const context = useContext(MainContext);
    const [categories, setCategories] = useState<ExtensionCategory[]>([]);

    useEffect(() => {
        const cats = Array.from(context.service.getCategories()) as ExtensionCategory[];
        cats.sort((a, b) => {
            if (a === b) return 0;
            if (a === 'Other') return 1;
            if (b === 'Other') return -1;
            return a.localeCompare(b);
        });
        setCategories(cats);
    }, []);

    return categories;
}
