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

import { describe, expect, it } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../../../support/test-providers';
import { disabledStatus, testUser } from '../../../support/trusted-publishing';
import { settingsServiceStub, testCustomer, testNamespace } from '../../../support/user-settings';
import { UserSettingsSidebar } from '../../../../../src/pages/user/settings/user-settings-sidebar';

const EMPTY_TEXT = "You don't belong to any namespace yet.";

describe('UserSettingsSidebar — navigation', () => {
    it('leaves Rate Limiting out for someone who belongs to no customer group', async () => {
        const { getCustomers, service } = settingsServiceStub({ status: disabledStatus });
        renderWithProviders(<UserSettingsSidebar />, { mainContext: { service, user: testUser } });

        await waitFor(() => expect(getCustomers).toHaveBeenCalled());
        expect(screen.queryByText('Rate Limiting')).not.toBeInTheDocument();
        expect(screen.getByText('Access Tokens')).toBeInTheDocument();
    });

    it('offers Rate Limiting to a member of a customer group', async () => {
        const { service } = settingsServiceStub({
            status: disabledStatus,
            customers: [testCustomer()]
        });
        renderWithProviders(<UserSettingsSidebar />, { mainContext: { service, user: testUser } });

        expect(await screen.findByText('Rate Limiting')).toBeInTheDocument();
    });
});

describe('UserSettingsSidebar — namespaces group', () => {
    it('shows the placeholder once the empty namespace list has loaded', async () => {
        const { service } = settingsServiceStub({ status: disabledStatus });
        renderWithProviders(<UserSettingsSidebar />, { mainContext: { service, user: testUser } });

        expect(await screen.findByText(EMPTY_TEXT)).toBeInTheDocument();
    });

    it('lists the namespaces instead of the placeholder when the user has some', async () => {
        const { service } = settingsServiceStub({
            status: disabledStatus,
            namespaces: [testNamespace({ name: 'redhat' })]
        });
        renderWithProviders(<UserSettingsSidebar />, { mainContext: { service, user: testUser } });

        expect(await screen.findByText('redhat')).toBeInTheDocument();
        expect(screen.queryByText(EMPTY_TEXT)).not.toBeInTheDocument();
    });

    it('holds the placeholder back while the namespace list is still loading', async () => {
        const { getNamespaces, service } = settingsServiceStub({ status: disabledStatus });
        // Never settles, so the query stays in its loading state for the whole assertion.
        getNamespaces.mockReturnValue(new Promise(() => {}));
        renderWithProviders(<UserSettingsSidebar />, { mainContext: { service, user: testUser } });

        await waitFor(() => expect(getNamespaces).toHaveBeenCalled());
        expect(screen.queryByText(EMPTY_TEXT)).not.toBeInTheDocument();
    });
});
