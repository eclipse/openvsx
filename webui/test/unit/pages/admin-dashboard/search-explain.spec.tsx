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

import { describe, it, expect, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../support/test-providers';
import { SearchExplainAdmin } from '../../../../src/pages/admin-dashboard/search-explain/search-explain';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { SearchExplain } from '../../../../src/extension-registry-types';

const explained: SearchExplain = {
    query: 'markdown',
    totalHits: 1024,
    references: { maxDownloadCount: 80771472, averageReviewRating: 0.12, oldestTimestamp: '2019-01-01T00:00Z' },
    entries: [
        {
            position: 0,
            namespace: 'yzhang',
            name: 'markdown-all-in-one',
            downloadCount: 640718,
            score: 1.9,
            textScore: 1.4,
            storedRelevance: 1.364,
            currentRelevance: 1.364,
            rating: 0.02,
            downloads: 0.008,
            recency: 0.656,
            unverified: false,
            deprecated: false
        }
    ]
};

const withService = (explainSearch: () => Promise<SearchExplain>) =>
    renderWithProviders(<SearchExplainAdmin />, {
        mainContext: { service: { admin: { explainSearch } } as unknown as ExtensionRegistryService }
    });

describe('SearchExplainAdmin', () => {
    // The page exists to answer "which half of the score put it there", so both halves have to be on it.
    it('shows the score split into its text and relevance halves', async () => {
        withService(() => Promise.resolve(explained));

        await userEvent.type(screen.getByLabelText('Search term'), 'markdown');
        await userEvent.click(screen.getByRole('button', { name: /explain/i }));

        expect(await screen.findByText('yzhang.markdown-all-in-one')).toBeInTheDocument();
        expect(screen.getByText('1.900')).toBeInTheDocument();
        expect(screen.getByText('1.400')).toBeInTheDocument();
        expect(screen.getByText('1.364')).toBeInTheDocument();
        expect(screen.getByText('0.656')).toBeInTheDocument();
    });

    // The reference values belong to the registry rather than any extension, and are the usual reason a
    // term contributes nothing - so they are stated rather than left to be inferred from a column of zeros.
    it('states what the terms are measured against', async () => {
        withService(() => Promise.resolve(explained));

        await userEvent.type(screen.getByLabelText('Search term'), 'markdown');
        await userEvent.click(screen.getByRole('button', { name: /explain/i }));

        expect(await screen.findByText('81M')).toBeInTheDocument();
        expect(screen.getByText('0.12')).toBeInTheDocument();
    });

    // An empty query matches everything, and a listing of everything in score order answers no question.
    it('does not search until a term is given', () => {
        const explainSearch = vi.fn(() => Promise.resolve(explained));
        withService(explainSearch);

        expect(explainSearch).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: /explain/i })).toBeDisabled();
    });
});
