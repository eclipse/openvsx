/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../support/test-providers';
import { ExtensionDetailOverview } from '../../../../src/pages/extension-detail/extension-detail-overview';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension, ExtensionReference } from '../../../../src/extension-registry-types';
import { PageSettings } from '../../../../src/page-settings';

const reference = (namespace: string, extension: string, available: boolean): ExtensionReference => ({
    namespace,
    extension,
    available
});

const extension = (overrides: Partial<Extension> = {}): Extension =>
    ({
        name: 'bar',
        namespace: 'foo',
        version: '1.0.0',
        displayName: 'Bar Tools',
        galleryColor: '',
        galleryTheme: '',
        // No readme file, so the component skips fetching one and renders immediately.
        files: {},
        downloads: {},
        downloadCount: 0,
        reviewCount: 0,
        deprecated: false,
        downloadable: false,
        versionAlias: [],
        allVersions: {},
        ...overrides
    }) as unknown as Extension;

function renderOverview(overrides: Partial<Extension> = {}) {
    const service = {} as unknown as ExtensionRegistryService;
    renderWithProviders(<ExtensionDetailOverview extension={extension(overrides)} selectVersion={vi.fn()} />, {
        mainContext: {
            service,
            pageSettings: { elements: {} } as PageSettings
        }
    });
}

describe('ExtensionDetailOverview - extension references', () => {
    it('links to a bundled extension that is available', () => {
        renderOverview({ bundledExtensions: [reference('foo', 'baz', true)] });

        const link = screen.getByRole('link', { name: 'foo.baz' });
        expect(link).toHaveAttribute('href', expect.stringContaining('/extension/foo/baz'));
    });

    it('shows an unavailable bundled extension as plain text with no link, saying so', () => {
        renderOverview({ bundledExtensions: [reference('foo', 'not-published', false)] });

        expect(screen.getByText('foo.not-published (not available)')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /not-published/ })).not.toBeInTheDocument();
    });

    it('applies the same available/unavailable distinction to dependencies', () => {
        renderOverview({
            dependencies: [reference('foo', 'available-dep', true), reference('foo', 'missing-dep', false)]
        });

        expect(screen.getByRole('link', { name: 'foo.available-dep' })).toBeInTheDocument();
        expect(screen.getByText('foo.missing-dep (not available)')).toBeInTheDocument();
    });

    it('does not render a "Bundled Extensions" section when there are none', () => {
        renderOverview();

        expect(screen.queryByText('Bundled Extensions')).not.toBeInTheDocument();
    });
});
