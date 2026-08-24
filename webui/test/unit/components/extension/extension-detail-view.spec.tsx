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
import { testUser } from '../../support/trusted-publishing';
import { ExtensionDetailView } from '../../../../src/components/extension/extension-detail-view';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension, VersionTargetPlatforms } from '../../../../src/extension-registry-types';
import { PageSettings } from '../../../../src/page-settings';

const version = (overrides: Partial<VersionTargetPlatforms> = {}): VersionTargetPlatforms => ({
    version: '1.0.0',
    targetPlatforms: [{ targetPlatform: 'universal', active: true, removed: false }],
    ...overrides
});

const extension = (versions: VersionTargetPlatforms[]): Extension =>
    ({
        name: 'bar',
        namespace: 'foo',
        version: '1.0.0',
        displayName: 'Bar Tools',
        files: {},
        downloadCount: 0,
        reviewCount: 0,
        deprecated: false,
        active: true,
        allTargetPlatformVersions: versions
    }) as unknown as Extension;

function renderDetail(versions: VersionTargetPlatforms[], onPurgeVersion?: () => Promise<unknown>) {
    const service = {
        getExtensionIcon: vi.fn().mockResolvedValue(null),
        getTrustedPublishingStatus: vi.fn().mockResolvedValue({ enabled: false, allowed: false })
    } as unknown as ExtensionRegistryService;
    renderWithProviders(
        <ExtensionDetailView
            extension={extension(versions)}
            onRemoveVersion={vi.fn().mockResolvedValue(undefined)}
            onVersionDeleted={vi.fn()}
            onPurgeVersion={onPurgeVersion}
        />,
        {
            mainContext: {
                service,
                user: testUser,
                pageSettings: { urls: { extensionDefaultIcon: '/icon.png' } } as PageSettings
            }
        }
    );
}

// Each danger-zone row repeats its title above the button, so query the button itself.
const dangerButton = (name: string) => screen.getByRole('button', { name });
const noDangerButton = (name: string) => screen.queryByRole('button', { name });

describe('ExtensionDetailView', () => {
    it('keeps the purge affordances out of the way without a purge handler', () => {
        renderDetail([version()]);

        expect(dangerButton('Delete all versions')).toBeInTheDocument();
        expect(noDangerButton('Purge all versions')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Purge version 1.0.0')).not.toBeInTheDocument();
    });

    it('offers purging once a purge handler is supplied', () => {
        renderDetail([version()], vi.fn().mockResolvedValue(undefined));

        expect(dangerButton('Purge all versions')).toBeInTheDocument();
        expect(screen.getByLabelText('Purge version 1.0.0')).toBeInTheDocument();
    });

    it('disables "Delete all versions" when nothing is left to delete', () => {
        const removed = version({ targetPlatforms: [{ targetPlatform: 'universal', active: false, removed: true }] });
        renderDetail([removed]);

        expect(dangerButton('Delete all versions')).toBeDisabled();
    });

    it('disables "Delete all versions" when the user published none of them', () => {
        renderDetail([version({ canDelete: false })]);

        expect(dangerButton('Delete all versions')).toBeDisabled();
    });

    it('enables "Delete all versions" when at least one is the user\'s to delete', () => {
        renderDetail([version({ canDelete: false }), version({ version: '1.1.0', canDelete: true })]);

        expect(dangerButton('Delete all versions')).toBeEnabled();
    });

    it('hides the danger zone entirely for an extension with no versions', () => {
        renderDetail([]);

        expect(screen.queryByText('Danger Zone')).not.toBeInTheDocument();
    });
});
