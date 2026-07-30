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
import { TrustedPublishingPromo } from '../../../../../src/pages/user/trusted-publishing/trusted-publishing-promo';
import { statusServiceStub, testUser } from '../../../support/trusted-publishing';

describe('TrustedPublishingPromo', () => {
    it('links to the trusted publishers settings tab when the feature is enabled', async () => {
        const { service } = statusServiceStub({ enabled: true, allowed: true });
        renderWithProviders(<TrustedPublishingPromo />, { mainContext: { service, user: testUser } });

        expect(await screen.findByRole('link', { name: /Set up a trusted publisher/ })).toHaveAttribute(
            'href',
            '/user-settings/trusted-publishers'
        );
    });

    it('stays hidden when trusted publishing is disabled', async () => {
        const { getTrustedPublishingStatus, service } = statusServiceStub({ enabled: false, allowed: false });
        renderWithProviders(<TrustedPublishingPromo />, { mainContext: { service, user: testUser } });

        await waitFor(() => expect(getTrustedPublishingStatus).toHaveBeenCalled());
        expect(screen.queryByRole('link', { name: /Set up a trusted publisher/ })).not.toBeInTheDocument();
    });

    it('stays hidden without a logged-in user', () => {
        const { getTrustedPublishingStatus, service } = statusServiceStub();
        renderWithProviders(<TrustedPublishingPromo />, { mainContext: { service } });

        expect(getTrustedPublishingStatus).not.toHaveBeenCalled();
        expect(screen.queryByRole('link', { name: /Set up a trusted publisher/ })).not.toBeInTheDocument();
    });
});
