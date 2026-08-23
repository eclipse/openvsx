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
import { renderWithProviders } from '../../support/test-providers';
import { ExtensionCardListItem } from '../../../../src/components/extension/extension-card-list-item';
import { Extension } from '../../../../src/extension-registry-types';

function buildExtension(overrides: Partial<Extension> = {}): Extension {
    return {
        namespaceUrl: 'https://example.test/api/foo',
        reviewsUrl: 'https://example.test/api/foo/bar/reviews',
        files: {},
        name: 'bar',
        namespace: 'foo',
        version: '1.0.0',
        targetPlatform: 'universal',
        publishedBy: { loginName: 'someone', tokensUrl: '', createTokenUrl: '' },
        verified: true,
        allVersions: {},
        downloadCount: 0,
        reviewCount: 0,
        versionAlias: [],
        timestamp: '2026-01-01T00:00:00Z',
        namespaceDisplayName: 'foo',
        galleryColor: '',
        galleryTheme: '',
        downloads: {},
        deprecated: false,
        downloadable: true,
        ...overrides
    };
}

describe('ExtensionCardListItem', () => {
    it('shows a namespace-verification hint when namespaceOwnershipConflict is set', () => {
        renderWithProviders(
            <ExtensionCardListItem
                extension={buildExtension({ active: false, namespaceOwnershipConflict: true })}
                routePrefix='extensions'
            />
        );

        expect(screen.getByText('Namespace needs verification')).toBeInTheDocument();
        expect(screen.queryByText('Deactivated')).not.toBeInTheDocument();
    });

    it('prioritizes the namespace-verification hint over the generic review status', () => {
        renderWithProviders(
            <ExtensionCardListItem
                extension={buildExtension({
                    active: false,
                    namespaceOwnershipConflict: true,
                    reviewStatus: 'under_review',
                    reviewMessage: 'Your extension is being reviewed. Please contact support for details.'
                })}
                routePrefix='extensions'
            />
        );

        expect(screen.getByText('Namespace needs verification')).toBeInTheDocument();
        expect(screen.queryByText('Under review')).not.toBeInTheDocument();
    });

    it('still shows Deleted for a removed extension even with a namespace ownership conflict', () => {
        renderWithProviders(
            <ExtensionCardListItem
                extension={buildExtension({ removed: true, namespaceOwnershipConflict: true })}
                routePrefix='extensions'
            />
        );

        expect(screen.getByText('Deleted')).toBeInTheDocument();
        expect(screen.queryByText('Namespace needs verification')).not.toBeInTheDocument();
    });

    it('falls back to Deactivated when there is no namespace ownership conflict', () => {
        renderWithProviders(
            <ExtensionCardListItem extension={buildExtension({ active: false })} routePrefix='extensions' />
        );

        expect(screen.getByText('Deactivated')).toBeInTheDocument();
        expect(screen.queryByText('Namespace needs verification')).not.toBeInTheDocument();
    });
});
