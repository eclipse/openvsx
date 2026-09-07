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

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../support/test-providers';
import { ExtensionCard } from '../../../src/components/extension-card';
import { Extension } from '../../../src/extension-registry-types';

const extension = (overrides: Partial<Extension> = {}): Extension =>
    ({
        namespaceUrl: 'https://example.test/api/foo',
        reviewsUrl: 'https://example.test/api/foo/bar/reviews',
        files: {},
        name: 'bar',
        namespace: 'foo',
        namespaceDisplayName: 'foo',
        displayName: 'Bar',
        version: '1.0.0',
        targetPlatform: 'universal',
        publishedBy: { loginName: 'someone', tokensUrl: '', createTokenUrl: '' },
        verified: true,
        allVersions: {},
        downloadCount: 0,
        reviewCount: 0,
        versionAlias: [],
        timestamp: '2026-01-01T00:00:00Z',
        downloads: {},
        deprecated: false,
        downloadable: true,
        ...overrides
    }) as unknown as Extension;

describe('ExtensionCard footer', () => {
    // The card that prompted this: five filled stars are an intrinsic width that cannot shrink, and
    // beside a long download count the pair outgrew the card and the count spilled past its edge.
    // One star and the rating is a bounded width, and says more than a row of icons to be counted.
    it('shows the rating as a single star and a number', () => {
        renderWithProviders(
            <ExtensionCard extension={extension({ averageRating: 5, reviewCount: 16, downloadCount: 41183269 })} />
        );

        expect(screen.getAllByTestId('StarIcon')).toHaveLength(1);
        expect(screen.getByText('5.0')).toBeInTheDocument();
        expect(screen.getByText('(16)')).toBeInTheDocument();
    });

    // The download count is what the eye compares across a grid of these, so it is never the side that
    // gives way - it keeps flexShrink: 0 while the rating beside it clips.
    it('keeps the download count whole alongside a rating', () => {
        renderWithProviders(
            <ExtensionCard extension={extension({ averageRating: 5, reviewCount: 16, downloadCount: 41183269 })} />
        );

        expect(screen.getByText('41M')).toBeInTheDocument();
    });

    // Nothing to say about a rating nobody has given: five grey stars claimed the width and stated a
    // score that does not exist.
    it('shows no rating at all when there are no reviews', () => {
        renderWithProviders(<ExtensionCard extension={extension({ downloadCount: 1234 })} />);

        expect(screen.queryByTestId('StarIcon')).not.toBeInTheDocument();
        expect(screen.getByText('1.2K')).toBeInTheDocument();
    });
});
