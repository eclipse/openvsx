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
import { ExtensionVersionTable } from '../../../../src/components/extension/extension-version-table';
import { VersionTargetPlatforms } from '../../../../src/extension-registry-types';

const version = (overrides: Partial<VersionTargetPlatforms> = {}): VersionTargetPlatforms => ({
    version: '1.0.0',
    targetPlatforms: [{ targetPlatform: 'universal', active: true, removed: false }],
    ...overrides
});

const removedVersion = version({
    version: '0.9.0',
    targetPlatforms: [{ targetPlatform: 'universal', active: false, removed: true }]
});

const renderTable = (
    versions: VersionTargetPlatforms[],
    onPurgeVersion?: () => void,
    extra: { rejected?: boolean } = {}
) =>
    renderWithProviders(
        <ExtensionVersionTable
            versions={versions}
            latestVersion='1.0.0'
            rejected={extra.rejected}
            page={0}
            onPageChange={vi.fn()}
            onDeleteVersion={vi.fn()}
            onPurgeVersion={onPurgeVersion}
        />
    );

describe('ExtensionVersionTable', () => {
    it('marks a version whose target platforms are all removed and blocks deleting it again', () => {
        renderTable([removedVersion]);

        expect(screen.getByText('Removed')).toBeInTheDocument();
        const deleteButton = screen.getByLabelText('Delete version 0.9.0');
        expect(deleteButton).toBeDisabled();
        expect(deleteButton).toHaveAttribute('title', 'Version already removed');
    });

    it('blocks deleting a version the current user did not publish', () => {
        renderTable([version({ canDelete: false })]);

        const deleteButton = screen.getByLabelText('Delete version 1.0.0');
        expect(deleteButton).toBeDisabled();
        expect(deleteButton).toHaveAttribute(
            'title',
            'Only the publisher or a namespace owner can delete this version'
        );
    });

    it('leaves the delete action available when nothing blocks it', () => {
        renderTable([version()]);

        expect(screen.getByLabelText('Delete version 1.0.0')).toBeEnabled();
    });

    it('marks every version of a rejected extension', () => {
        renderTable([version()], undefined, { rejected: true });

        expect(screen.getByText('Rejected')).toBeInTheDocument();
        expect(screen.queryByText('Latest')).not.toBeInTheDocument();
    });

    it('leaves a removed version marked as removed even when the extension is rejected', () => {
        renderTable([removedVersion], undefined, { rejected: true });

        expect(screen.getByText('Removed')).toBeInTheDocument();
        expect(screen.queryByText('Rejected')).not.toBeInTheDocument();
    });

    it('offers the purge action only when a purge handler is supplied', () => {
        const { unmount } = renderTable([version()]);
        expect(screen.queryByLabelText('Purge version 1.0.0')).not.toBeInTheDocument();
        unmount();

        renderTable([version()], vi.fn());
        expect(screen.getByLabelText('Purge version 1.0.0')).toBeInTheDocument();
    });
});
