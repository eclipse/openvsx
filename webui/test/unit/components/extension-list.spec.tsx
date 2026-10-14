/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../support/test-providers';
import { createTestViewport } from '../support/viewport';
import { ExtensionList } from '../../../src/components/extension-list';
import { ExtensionFilter, ExtensionRegistryService } from '../../../src/extension-registry-service';
import { SearchEntry, SearchResult } from '../../../src/extension-registry-types';

const PAGE_SIZE = 2;

// No icon file, so only the list's sentinel is observed by the test viewport.
const entry = (idx: number): SearchEntry =>
    ({
        namespace: 'foo',
        name: `ext${idx}`,
        displayName: `Extension ${idx}`,
        version: '1.0.0',
        targetPlatform: 'universal',
        files: {}
    }) as unknown as SearchEntry;

const page = (offset: number, totalSize: number): SearchResult => ({
    offset,
    totalSize,
    extensions: Array.from({ length: Math.min(PAGE_SIZE, totalSize - offset) }, (_, i) => entry(offset + i))
});

function renderList(search: (abortController: AbortController, filter: ExtensionFilter) => Promise<SearchResult>) {
    const viewport = createTestViewport();
    const spy = vi.fn(search);
    renderWithProviders(<ExtensionList filter={{ query: 'q', offset: 0, size: PAGE_SIZE }} onUpdate={() => {}} />, {
        mainContext: { service: { search: spy } as unknown as ExtensionRegistryService },
        viewport: viewport.observer
    });
    return { search: spy, viewport };
}

const offsets = (search: ReturnType<typeof vi.fn>) => search.mock.calls.map(([, filter]) => filter.offset);

describe('ExtensionList', () => {
    it('loads the next page only once the end of the list comes near the viewport', async () => {
        const { search, viewport } = renderList(async (_, filter) => page(filter.offset!, 4));
        expect(await screen.findByText('Extension 1')).toBeInTheDocument();
        expect(offsets(search)).toEqual([0]);
        expect(viewport.observed()).toEqual([expect.objectContaining({ rootMargin: '200px' })]);

        viewport.setInView(true);

        expect(await screen.findByText('Extension 3')).toBeInTheDocument();
        expect(offsets(search)).toEqual([0, 2]);
    });

    it('keeps loading pages while the end of the list stays in view, and stops at the last one', async () => {
        const { search, viewport } = renderList(async (_, filter) => page(filter.offset!, 5));
        await screen.findByText('Extension 1');

        viewport.setInView(true);

        expect(await screen.findByText('Extension 4')).toBeInTheDocument();
        expect(offsets(search)).toEqual([0, 2, 4]);
        expect(viewport.observed()).toHaveLength(0);
    });

    it('does not request another page while one is loading', async () => {
        let release: () => void = () => {};
        const { search, viewport } = renderList((_, filter) =>
            filter.offset === 0
                ? Promise.resolve(page(0, 4))
                : new Promise(resolve => (release = () => resolve(page(filter.offset!, 4))))
        );
        await screen.findByText('Extension 1');

        viewport.setInView(true);
        await waitFor(() => expect(offsets(search)).toEqual([0, 2]));
        viewport.setInView(false);
        viewport.setInView(true);
        expect(offsets(search)).toEqual([0, 2]);

        release();
        expect(await screen.findByText('Extension 3')).toBeInTheDocument();
        expect(offsets(search)).toEqual([0, 2]);
    });

    it('requests nothing more when the first page holds every result', async () => {
        const { search, viewport } = renderList(async () => page(0, 2));
        await screen.findByText('Extension 1');

        viewport.setInView(true);

        expect(offsets(search)).toEqual([0]);
        expect(viewport.observed()).toHaveLength(0);
    });
});
