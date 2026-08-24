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
import { renderWithProviders } from '../../../support/test-providers';
import { testUser } from '../../../support/trusted-publishing';
import { UserSettingsExtensions } from '../../../../../src/pages/user/extensions/user-settings-extensions';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';
import { Extension, UserData } from '../../../../../src/extension-registry-types';
import { PageSettings } from '../../../../../src/page-settings';

const extension = (name: string): Extension =>
    ({
        name,
        namespace: 'foo',
        version: '1.0.0',
        displayName: name,
        files: {},
        downloadCount: 0,
        reviewCount: 0,
        deprecated: false
    }) as Extension;

function renderExtensions(extensions: Extension[]) {
    const getExtensions = vi.fn().mockResolvedValue(extensions);
    const service = {
        getExtensions,
        getExtensionIcon: vi.fn().mockResolvedValue(null),
        getRegistryVersion: vi.fn().mockResolvedValue({})
    } as unknown as ExtensionRegistryService;
    renderWithProviders(<UserSettingsExtensions />, {
        mainContext: {
            service,
            user: { ...testUser, publishUrl: '/publish' } as UserData,
            pageSettings: { urls: { extensionDefaultIcon: '/icon.png' } } as PageSettings
        }
    });
    return { getExtensions };
}

describe('UserSettingsExtensions', () => {
    it('says so when the user has published nothing yet', async () => {
        const { getExtensions } = renderExtensions([]);

        await waitFor(() => expect(getExtensions).toHaveBeenCalled());
        expect(await screen.findByText("You haven't published any extensions yet.")).toBeInTheDocument();
    });

    it('links each card at the user settings extension route', async () => {
        renderExtensions([extension('bar')]);

        expect(await screen.findByLabelText('bar')).toHaveAttribute('href', '/user-settings/extensions/foo/bar');
        expect(screen.queryByText("You haven't published any extensions yet.")).not.toBeInTheDocument();
    });
});
