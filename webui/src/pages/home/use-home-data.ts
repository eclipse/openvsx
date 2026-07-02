/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useContext, useEffect, useState } from 'react';
import { MainContext } from '../../context';
import { ExtensionCategory, SearchEntry, SearchResult, SortOrder, isError } from '../../extension-registry-types';
import { HomeCuratedSection } from '../../page-settings';

/** Number of extensions fetched for each curated row. */
export const CURATED_SIZE = 6;

/** Categories that add little value on the home page grid. */
const EXCLUDED_CATEGORIES = new Set(['Other', 'SCM Providers', 'Extension Packs']);

/** Curated rows shown when the consumer does not configure `home.curatedSections`. */
export const DEFAULT_CURATED_SECTIONS: HomeCuratedSection[] = [
    { title: 'Most downloaded', subtitle: 'The extensions developers rely on every day', sortBy: 'downloadCount' },
    { title: 'Recently updated', subtitle: 'Fresh releases from publishers this week', sortBy: 'timestamp' }
];

export interface CuratedRow extends HomeCuratedSection {
    extensions: SearchEntry[];
    loading: boolean;
}

/**
 * Loads the home page data: the browsable category list (excluding a few noisy
 * ones) and the curated extension rows, each fetched with its configured
 * ordering. Rows start in a loading state and fill in as requests resolve.
 */
export function useHomeData(curatedSections: HomeCuratedSection[]): {
    categories: ExtensionCategory[];
    rows: CuratedRow[];
} {
    const { service } = useContext(MainContext);
    const [categories, setCategories] = useState<ExtensionCategory[]>([]);
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

    return { categories, rows };
}
