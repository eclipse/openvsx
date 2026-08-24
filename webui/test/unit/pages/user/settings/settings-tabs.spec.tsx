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
import { waitFor } from '@testing-library/react';
import { renderHookWithProviders } from '../../../support/test-providers';
import { disabledStatus, enabledStatus, testUser } from '../../../support/trusted-publishing';
import { settingsServiceStub, testCustomer } from '../../../support/user-settings';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';
import { useSettingsTabs } from '../../../../../src/pages/user/settings/settings-tabs';

const renderTabs = (service: ExtensionRegistryService, user?: typeof testUser) =>
    renderHookWithProviders(() => useSettingsTabs(), { mainContext: { service, user } });

describe('useSettingsTabs — trusted publishing gate', () => {
    it('includes the Trusted Publishers tab when the status reports the feature enabled', async () => {
        const { service } = settingsServiceStub({ status: enabledStatus });
        const { result } = renderTabs(service, testUser);

        await waitFor(() => expect(result.current.map(tab => tab.value)).toContain('trusted-publishers'));
    });

    it('omits the tab when the status reports the feature disabled', async () => {
        const { getTrustedPublishingStatus, service } = settingsServiceStub({ status: disabledStatus });
        const { result } = renderTabs(service, testUser);

        await waitFor(() => expect(getTrustedPublishingStatus).toHaveBeenCalled());
        expect(result.current.map(tab => tab.value)).not.toContain('trusted-publishers');
        expect(result.current.map(tab => tab.value)).toContain('tokens');
    });

    it('omits the tab without a logged-in user, without querying the status', () => {
        const { getTrustedPublishingStatus, service } = settingsServiceStub({ status: enabledStatus });
        const { result } = renderTabs(service);

        expect(getTrustedPublishingStatus).not.toHaveBeenCalled();
        expect(result.current.map(tab => tab.value)).not.toContain('trusted-publishers');
    });
});

describe('useSettingsTabs — rate limiting gate', () => {
    it('includes the Rate Limiting tab only for members of a customer group', async () => {
        const { service } = settingsServiceStub({ status: disabledStatus, customers: [testCustomer()] });
        const { result } = renderTabs(service, testUser);

        await waitFor(() => expect(result.current.map(tab => tab.value)).toContain('customers'));
    });

    it('omits the Rate Limiting tab when the user belongs to no customer group', async () => {
        const { getCustomers, service } = settingsServiceStub({ status: disabledStatus });
        const { result } = renderTabs(service, testUser);

        await waitFor(() => expect(getCustomers).toHaveBeenCalled());
        expect(result.current.map(tab => tab.value)).not.toContain('customers');
    });
});
