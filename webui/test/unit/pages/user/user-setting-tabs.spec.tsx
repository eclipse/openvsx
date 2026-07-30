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
import { renderWithProviders } from '../../support/test-providers';
import { UserSettingTabs } from '../../../../src/pages/user/user-setting-tabs';
import { statusServiceStub, testUser } from '../../support/trusted-publishing';

describe('UserSettingTabs — trusted publishing gate', () => {
    it('shows the Trusted Publishers tab when the status reports the feature enabled', async () => {
        const { service } = statusServiceStub({ enabled: true, allowed: true });
        renderWithProviders(<UserSettingTabs />, { mainContext: { service, user: testUser } });

        expect(await screen.findByRole('tab', { name: 'Trusted Publishers' })).toBeInTheDocument();
    });

    it('hides the tab when the status reports the feature disabled', async () => {
        const { getTrustedPublishingStatus, service } = statusServiceStub({ enabled: false, allowed: false });
        renderWithProviders(<UserSettingTabs />, { mainContext: { service, user: testUser } });

        await waitFor(() => expect(getTrustedPublishingStatus).toHaveBeenCalled());
        expect(screen.queryByRole('tab', { name: 'Trusted Publishers' })).not.toBeInTheDocument();
        expect(screen.getByRole('tab', { name: 'Access Tokens' })).toBeInTheDocument();
    });

    it('hides the tab without a logged-in user, without querying the status', () => {
        const { getTrustedPublishingStatus, service } = statusServiceStub({ enabled: true, allowed: true });
        renderWithProviders(<UserSettingTabs />, { mainContext: { service } });

        expect(getTrustedPublishingStatus).not.toHaveBeenCalled();
        expect(screen.queryByRole('tab', { name: 'Trusted Publishers' })).not.toBeInTheDocument();
    });
});
