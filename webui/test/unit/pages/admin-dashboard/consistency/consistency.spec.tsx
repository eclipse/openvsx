/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../support/test-providers';
import { DataConsistency } from '../../../../../src/pages/admin-dashboard/consistency/consistency';
import { ExtensionRegistryService, AdminService } from '../../../../../src/extension-registry-service';
import { ConsistencyCheck, ConsistencyFinding } from '../../../../../src/extension-registry-types';

const CHECK: ConsistencyCheck = {
    id: 'extension-active-flag',
    name: 'Extension active flag',
    description: 'Extensions whose active flag disagrees with whether any of their versions is active.',
    currentFindingsCount: 1
};

const FINDING: ConsistencyFinding = {
    entityId: 42,
    label: 'acme.foo',
    detail: 'marked active, but no version of it is active'
};

function serviceWith(overrides: Partial<AdminService> = {}): ExtensionRegistryService {
    const admin = {
        getConsistencyChecks: vi.fn().mockResolvedValue({ checks: [CHECK] }),
        getConsistencyFindings: vi.fn().mockResolvedValue({ findings: [FINDING] }),
        fixConsistencyFindings: vi.fn().mockResolvedValue({ success: 'Fixed 1 finding(s).' }),
        fixConsistencyFinding: vi.fn().mockResolvedValue({ success: 'Fixed entity 42.' }),
        ...overrides
    } as unknown as AdminService;

    return { serverUrl: 'https://open-vsx.org', admin } as ExtensionRegistryService;
}

describe('DataConsistency', () => {
    it('shows a finding count badge for each registered check', async () => {
        renderWithProviders(<DataConsistency />, { mainContext: { service: serviceWith() } });

        expect(await screen.findByText('Extension active flag')).toBeInTheDocument();
        expect(screen.getByText('1 found')).toBeInTheDocument();
    });

    it('loads and displays findings only once the check card is expanded', async () => {
        const service = serviceWith();
        const ue = userEvent.setup();
        renderWithProviders(<DataConsistency />, { mainContext: { service } });

        await screen.findByText('Extension active flag');
        expect(service.admin.getConsistencyFindings).not.toHaveBeenCalled();

        await ue.click(screen.getByText('Extension active flag'));

        expect(await screen.findByText('acme.foo')).toBeInTheDocument();
        expect(screen.getByText('marked active, but no version of it is active')).toBeInTheDocument();
        expect(service.admin.getConsistencyFindings).toHaveBeenCalledWith(expect.anything(), 'extension-active-flag');
    });

    it('fixes a single finding and refreshes the list', async () => {
        const service = serviceWith();
        const ue = userEvent.setup();
        renderWithProviders(<DataConsistency />, { mainContext: { service } });

        await ue.click(await screen.findByText('Extension active flag'));
        await screen.findByText('acme.foo');

        await ue.click(screen.getByRole('button', { name: 'Fix' }));

        expect(service.admin.fixConsistencyFinding).toHaveBeenCalledWith('extension-active-flag', 42);
        await waitFor(() => expect(service.admin.getConsistencyChecks).toHaveBeenCalledTimes(2));
    });

    it('offers "Fix all" directly on the check card, without needing to expand it', async () => {
        const service = serviceWith();
        const ue = userEvent.setup();
        renderWithProviders(<DataConsistency />, { mainContext: { service } });

        await screen.findByText('Extension active flag');
        expect(service.admin.getConsistencyFindings).not.toHaveBeenCalled();

        await ue.click(screen.getByRole('button', { name: 'Fix all' }));

        expect(service.admin.fixConsistencyFindings).toHaveBeenCalledWith('extension-active-flag');
        await waitFor(() => expect(service.admin.getConsistencyChecks).toHaveBeenCalledTimes(2));
        // Clicking "Fix all" must not also expand the card.
        expect(service.admin.getConsistencyFindings).not.toHaveBeenCalled();
    });

    it('refreshes the overview on demand without triggering any server action', async () => {
        const service = serviceWith();
        const ue = userEvent.setup();
        renderWithProviders(<DataConsistency />, { mainContext: { service } });

        await screen.findByText('Extension active flag');
        await ue.click(screen.getByRole('button', { name: 'Refresh' }));

        await waitFor(() => expect(service.admin.getConsistencyChecks).toHaveBeenCalledTimes(2));
    });
});
