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
import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HelmetProvider } from 'react-helmet-async';
import { renderWithProviders } from '../../support/test-providers';
import { testUser } from '../../support/trusted-publishing';
import { PublishPage } from '../../../../src/pages/publish/publish-page';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension, UserData } from '../../../../src/extension-registry-types';
import { PageSettings } from '../../../../src/page-settings';

const vsix = (name: string) => new File(['package'], name, { type: 'application/vsix' });

function renderPage(user?: UserData) {
    const publishExtension = vi
        .fn()
        .mockResolvedValue({ name: 'bar', namespace: 'foo', displayName: 'Bar', files: {} } as unknown as Extension);
    renderWithProviders(
        <HelmetProvider>
            <PublishPage />
        </HelmetProvider>,
        {
            mainContext: {
                service: {
                    publishExtension,
                    getExtensions: vi.fn().mockResolvedValue([]),
                    getExtensionIcon: vi.fn().mockResolvedValue(null)
                } as unknown as ExtensionRegistryService,
                user,
                pageSettings: {
                    pageTitle: 'Open VSX',
                    urls: { extensionDefaultIcon: '/icon.png' }
                } as PageSettings
            }
        }
    );
    return { publishExtension };
}

describe('PublishPage', () => {
    it('uploads every package picked at once, without a confirmation step', async () => {
        const { publishExtension } = renderPage(testUser);

        const input = screen.getByLabelText('Extension packages');
        expect(input).toHaveAttribute('multiple');
        await userEvent.upload(input, [vsix('one.vsix'), vsix('two.vsix')]);

        await waitFor(() => expect(publishExtension).toHaveBeenCalledTimes(2));
    });

    it('publishes a package dropped on its drop area', async () => {
        const { publishExtension } = renderPage(testUser);

        fireEvent.drop(screen.getByText('Drag & drop your extensions here'), {
            dataTransfer: { types: ['Files'], files: [vsix('one.vsix')] }
        });

        await waitFor(() => expect(publishExtension).toHaveBeenCalledOnce());
    });

    it('asks a signed-out visitor to log in instead of offering the dropzone', () => {
        renderPage();

        expect(screen.getByText(/to publish an extension/)).toBeInTheDocument();
        expect(screen.queryByLabelText('Extension packages')).not.toBeInTheDocument();
    });

    it('shows the queue inline above its drop area once something is uploading', async () => {
        renderPage(testUser);

        await userEvent.upload(screen.getByLabelText('Extension packages'), [vsix('one.vsix')]);

        expect(await screen.findByLabelText('Publishing queue')).toBeInTheDocument();
    });

    it('keeps the command line alternative alongside the dropzone', () => {
        renderPage(testUser);

        expect(screen.getByText(/ovsx publish my-extension.vsix/)).toBeInTheDocument();
    });
});
