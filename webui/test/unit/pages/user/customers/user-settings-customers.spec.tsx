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
import { renderWithProviders } from '../../../support/test-providers';
import { disabledStatus, testUser } from '../../../support/trusted-publishing';
import { settingsServiceStub, testCustomer } from '../../../support/user-settings';
import { UserSettingsCustomers } from '../../../../../src/pages/user/customers/user-settings-customers';
import { Customer } from '../../../../../src/extension-registry-types';

// The real detail pulls in @mui/x-charts, whose ESM build cannot be resolved under vitest.
// This page owns the empty state and the chooser, so a stub naming the chosen customer is enough.
vi.mock('../../../../../src/pages/user/customers/user-settings-customer-detail', () => ({
    UserSettingsCustomerDetail: ({ customer }: { customer: Customer }) => <div>detail for {customer.name}</div>
}));

const customer = (name: string): Customer => testCustomer({ name });

function renderCustomers(customers: Customer[]) {
    const { getCustomers, service } = settingsServiceStub({ status: disabledStatus, customers });
    renderWithProviders(<UserSettingsCustomers />, { mainContext: { service, user: testUser } });
    return { getCustomers };
}

describe('UserSettingsCustomers', () => {
    it('says so when the user belongs to no customer group', async () => {
        const { getCustomers } = renderCustomers([]);

        await waitFor(() => expect(getCustomers).toHaveBeenCalled());
        expect(
            await screen.findByText('You are not a member of any rate limiting customer group.')
        ).toBeInTheDocument();
    });

    it('shows the only group straight away, without a chooser', async () => {
        renderCustomers([customer('acme')]);

        expect(await screen.findByText('detail for acme')).toBeInTheDocument();
        // With one group the pill row is pointless, so the name appears only in the detail.
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('offers a pill per group and switches the detail when one is picked', async () => {
        renderCustomers([customer('acme'), customer('globex')]);

        expect(await screen.findByText('detail for acme')).toBeInTheDocument();

        await userEvent.click(screen.getByText('globex'));

        expect(await screen.findByText('detail for globex')).toBeInTheDocument();
        expect(screen.queryByText('detail for acme')).not.toBeInTheDocument();
    });
});
