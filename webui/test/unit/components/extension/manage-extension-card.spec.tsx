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
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../support/test-providers';
import { ManageExtensionCard } from '../../../../src/components/extension/manage-extension-card';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension } from '../../../../src/extension-registry-types';
import { PageSettings } from '../../../../src/page-settings';

const extension = (overrides: Partial<Extension> = {}): Extension =>
    ({
        name: 'bar',
        namespace: 'foo',
        version: '1.0.0',
        displayName: 'Bar Tools',
        description: 'Tools that make bars better.',
        files: {},
        downloadCount: 12,
        reviewCount: 0,
        deprecated: false,
        verified: true,
        ...overrides
    }) as Extension;

const renderCard = (ext: Extension) =>
    renderWithProviders(<ManageExtensionCard extension={ext} routePrefix='/user-settings/extensions' />, {
        mainContext: {
            service: { getExtensionIcon: vi.fn().mockResolvedValue(null) } as unknown as ExtensionRegistryService,
            pageSettings: { urls: {} } as PageSettings
        }
    });

describe('ManageExtensionCard', () => {
    it('renders the shared card content, description included, linked to the management route', async () => {
        renderCard(extension());

        expect(await screen.findByText('Bar Tools')).toBeInTheDocument();
        expect(screen.getByText('Tools that make bars better.')).toBeInTheDocument();
        expect(screen.getByLabelText('Bar Tools')).toHaveAttribute('href', '/user-settings/extensions/foo/bar');
    });

    it('shows the publishing state in the footer when the extension is not simply public', async () => {
        renderCard(extension({ active: false }));

        expect(await screen.findByText('Deactivated')).toBeInTheDocument();
    });

    it('names the unverified namespace as the cause instead of just calling it deactivated', async () => {
        renderCard(extension({ active: false, namespaceOwnershipConflict: true }));

        // The status names the actual, actionable cause rather than just "Deactivated"; the card is
        // left in colour to match (greyscale would swallow the warning tone).
        expect(await screen.findByText('Namespace not verified')).toBeInTheDocument();
        expect(screen.getByTitle('Needs attention')).toBeInTheDocument();
        expect(screen.queryByText('Deactivated')).not.toBeInTheDocument();
    });

    it('leaves the footer to the shared card when the extension is public', async () => {
        renderCard(extension());

        expect(await screen.findByText('Bar Tools')).toBeInTheDocument();
        expect(screen.queryByText('Deactivated')).not.toBeInTheDocument();
    });
});
