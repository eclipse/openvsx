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
import { testUser } from '../../../support/trusted-publishing';
import { UserSettingsTokens } from '../../../../../src/pages/user/tokens/user-settings-tokens';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';
import { PageSettings } from '../../../../../src/page-settings';
import { PersonalAccessToken } from '../../../../../src/extension-registry-types';

const token: PersonalAccessToken = {
    id: 1,
    description: 'publishing token',
    createdTimestamp: '2026-01-01T00:00:00Z',
    deleteTokenUrl: ''
};

function renderTokens() {
    const deleteAccessToken = vi.fn().mockResolvedValue(undefined);
    const getAccessTokens = vi.fn().mockResolvedValue([token]);
    const service = {
        getAccessTokens,
        deleteAccessToken,
        getTrustedPublishingStatus: vi.fn().mockResolvedValue({ enabled: false, allowed: false })
    } as unknown as ExtensionRegistryService;
    renderWithProviders(<UserSettingsTokens />, {
        mainContext: { service, user: testUser, pageSettings: {} as PageSettings }
    });
    return { deleteAccessToken };
}

describe('UserSettingsTokens', () => {
    it('revokes a token only after the confirmation is accepted', async () => {
        const { deleteAccessToken } = renderTokens();

        await userEvent.click(await screen.findByLabelText('Revoke token publishing token'));

        expect(await screen.findByText('Revoke access token')).toBeInTheDocument();
        expect(deleteAccessToken).not.toHaveBeenCalled();

        await userEvent.click(screen.getByText('Revoke'));
        await waitFor(() => expect(deleteAccessToken).toHaveBeenCalledWith(expect.anything(), token));
    });

    it('keeps the token when the confirmation is dismissed', async () => {
        const { deleteAccessToken } = renderTokens();

        await userEvent.click(await screen.findByLabelText('Revoke token publishing token'));
        await userEvent.click(await screen.findByText('Cancel'));

        await waitFor(() => expect(screen.queryByText('Revoke access token')).not.toBeInTheDocument());
        expect(deleteAccessToken).not.toHaveBeenCalled();
    });
});
