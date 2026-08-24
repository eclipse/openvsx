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
import { renderWithProviders } from '../../support/test-providers';
import { namespaceDetails, testNamespace } from '../../support/user-settings';
import { NamespaceLogo } from '../../../../src/components/namespace/namespace-logo';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { PageSettings } from '../../../../src/page-settings';

function renderLogo(logo?: string) {
    const getNamespaceDetails = vi.fn().mockResolvedValue(namespaceDetails({ logo }));
    renderWithProviders(<NamespaceLogo namespace={testNamespace()} />, {
        mainContext: {
            service: { getNamespaceDetails } as unknown as ExtensionRegistryService,
            pageSettings: { urls: { extensionDefaultIcon: '/default-icon.png' } } as PageSettings
        }
    });
    return { getNamespaceDetails };
}

describe('NamespaceLogo', () => {
    it('says there is no logo instead of falling back to the default icon', async () => {
        const { getNamespaceDetails } = renderLogo();

        await waitFor(() => expect(getNamespaceDetails).toHaveBeenCalled());
        expect(await screen.findByText('No logo')).toBeInTheDocument();
        expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });

    it('shows the logo when the namespace has one', async () => {
        renderLogo('https://example.test/logo.png');

        expect(await screen.findByAltText('Foo logo')).toHaveAttribute('src', 'https://example.test/logo.png');
        expect(screen.queryByText('No logo')).not.toBeInTheDocument();
    });
});
