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
import userEvent from '@testing-library/user-event';
import { Route, Routes, useLocation } from 'react-router';
import { renderWithProviders } from '../../../support/test-providers';
import { testUser } from '../../../support/trusted-publishing';
import {
    ExtensionSettings,
    ExtensionSettingsBackState
} from '../../../../../src/pages/user/extensions/extension-settings';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';
import { Extension } from '../../../../../src/extension-registry-types';
import { PageSettings } from '../../../../../src/page-settings';

const extension = {
    name: 'bar',
    namespace: 'foo',
    version: '1.0.0',
    displayName: 'Bar Tools',
    files: {},
    downloadCount: 0,
    reviewCount: 0,
    deprecated: false,
    active: true,
    allTargetPlatformVersions: []
} as unknown as Extension;

const CurrentPath = () => <span data-testid='path'>{useLocation().pathname}</span>;

function renderSettings(options: { backState?: ExtensionSettingsBackState; error?: unknown } = {}) {
    const getExtension = options.error
        ? vi.fn().mockRejectedValue(options.error)
        : vi.fn().mockResolvedValue(extension);
    const service = {
        getExtension,
        getExtensionIcon: vi.fn().mockResolvedValue(null),
        getTrustedPublishingStatus: vi.fn().mockResolvedValue({ enabled: false, allowed: false })
    } as unknown as ExtensionRegistryService;
    const handleError = vi.fn();
    renderWithProviders(
        <>
            <CurrentPath />
            <Routes>
                <Route path='*' element={<ExtensionSettings namespace='foo' extension='bar' />} />
            </Routes>
        </>,
        {
            mainContext: {
                service,
                user: testUser,
                handleError,
                pageSettings: { urls: { extensionDefaultIcon: '/icon.png' }, elements: {} } as PageSettings
            },
            route: { pathname: '/user-settings/extensions/foo/bar', state: options.backState }
        }
    );
    return { handleError };
}

describe('ExtensionSettings', () => {
    it('returns to the extensions tab by default', async () => {
        renderSettings();

        const back = await screen.findByText('Back to your extensions');
        await userEvent.click(back);

        await waitFor(() => expect(screen.getByTestId('path')).toHaveTextContent('/user-settings/extensions'));
    });

    it('returns to wherever the card that linked here came from', async () => {
        renderSettings({ backState: { backTo: '/user-settings/namespaces/foo', backLabel: 'Back to foo' } });

        await userEvent.click(await screen.findByText('Back to foo'));

        await waitFor(() => expect(screen.getByTestId('path')).toHaveTextContent('/user-settings/namespaces/foo'));
    });

    it('reports a failed lookup and leaves a dead extension page on 404', async () => {
        const { handleError } = renderSettings({ error: { status: 404 } });

        await waitFor(() => expect(handleError).toHaveBeenCalled());
        await waitFor(() => expect(screen.getByTestId('path')).toHaveTextContent('/user-settings/extensions'));
    });

    it('stays put when the lookup fails for another reason', async () => {
        const { handleError } = renderSettings({ error: { status: 500 } });

        await waitFor(() => expect(handleError).toHaveBeenCalled());
        expect(screen.getByTestId('path')).toHaveTextContent('/user-settings/extensions/foo/bar');
    });
});
