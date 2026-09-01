/******************************************************************************
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
import userEvent from '@testing-library/user-event';
import { SearchIndexAdmin } from '../../../../src/pages/admin-dashboard/search-index/search-index';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { SearchIndex } from '../../../../src/extension-registry-types';
import { renderWithProviders } from '../../support/test-providers';

const index = (overrides: Partial<SearchIndex> = {}): SearchIndex => ({
    enabled: true,
    implementation: 'elasticsearch',
    indexExists: true,
    indexedDocuments: 1234,
    activeExtensions: 1234,
    maxResultWindow: 10000,
    ...overrides
});

// not named render*, so the testing-library naming rule does not treat the service stub it returns
// as a render result
const mountPage = (data: SearchIndex, updateSearchIndex = vi.fn().mockResolvedValue({ success: 'ok' })) => {
    const admin = {
        getSearchIndex: vi.fn().mockResolvedValue(data),
        updateSearchIndex
    };
    renderWithProviders(<SearchIndexAdmin />, {
        mainContext: { service: { admin } as unknown as ExtensionRegistryService }
    });
    return admin;
};

describe('SearchIndexAdmin', () => {
    it('shows what the index holds against what it is built from', async () => {
        mountPage(index({ indexedDocuments: 1200, activeExtensions: 1234 }));

        expect(await screen.findByText('1,200')).toBeInTheDocument();
        expect(screen.getByText('1,234')).toBeInTheDocument();
    });

    // The whole reason for the page: an index missing entries answers searches fine, just emptily, which
    // looks exactly like a registry with nothing in it unless both counts are visible together.
    it('calls out extensions that are missing from the index', async () => {
        mountPage(index({ indexedDocuments: 1200, activeExtensions: 1234 }));

        expect(await screen.findByText(/34 active extensions are missing from the index/)).toBeInTheDocument();
    });

    it('reports a healthy index when the counts agree', async () => {
        mountPage(index());

        expect(await screen.findByText('Every active extension is indexed.')).toBeInTheDocument();
    });

    it('calls out an index holding more than exists', async () => {
        mountPage(index({ indexedDocuments: 1240, activeExtensions: 1234 }));

        expect(await screen.findByText(/holds 6 more entries than there are active extensions/)).toBeInTheDocument();
    });

    it('reports a missing index rather than showing it as empty', async () => {
        mountPage(index({ indexExists: false, indexedDocuments: undefined }));

        expect(await screen.findByText(/The index does not exist/)).toBeInTheDocument();
    });

    it('says there is nothing to rebuild when searches come from the database', async () => {
        mountPage(index({ implementation: 'database', indexExists: false, indexedDocuments: undefined }));

        expect(await screen.findByText(/no index to report on or rebuild/)).toBeInTheDocument();
    });

    it('says so when searching is switched off entirely', async () => {
        mountPage(index({ enabled: false }));

        expect(await screen.findByText('Searching is disabled on this registry.')).toBeInTheDocument();
    });

    // The database engine's updateSearchIndex only evicts a cache, so the button would report a rebuild
    // that never happened - right next to a banner saying there is no index to rebuild.
    it('offers no rebuild when searches come from the database', async () => {
        mountPage(index({ implementation: 'database', indexExists: false, indexedDocuments: undefined }));

        await screen.findByText(/no index to report on or rebuild/);
        expect(screen.queryByRole('button', { name: 'Update search index' })).not.toBeInTheDocument();
    });

    it('offers no rebuild when searching is switched off entirely', async () => {
        mountPage(index({ enabled: false }));

        await screen.findByText('Searching is disabled on this registry.');
        expect(screen.queryByRole('button', { name: 'Update search index' })).not.toBeInTheDocument();
    });

    // A missing index is the one case where rebuilding is exactly the remedy, so it stays on offer.
    it('still offers the rebuild when the index is missing', async () => {
        mountPage(index({ indexExists: false, indexedDocuments: undefined }));

        expect(await screen.findByRole('button', { name: 'Update search index' })).toBeInTheDocument();
    });

    it('rebuilds the index and refetches the statistics afterwards', async () => {
        const user = userEvent.setup();
        const adminService = mountPage(index({ indexedDocuments: 1200, activeExtensions: 1234 }));

        await user.click(await screen.findByRole('button', { name: 'Update search index' }));

        await waitFor(() => expect(adminService.updateSearchIndex).toHaveBeenCalledOnce());
        // the counts are the only way to tell whether the rebuild achieved anything
        await waitFor(() => expect(adminService.getSearchIndex).toHaveBeenCalledTimes(2));
    });
});
