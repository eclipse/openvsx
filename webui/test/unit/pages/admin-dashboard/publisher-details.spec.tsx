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
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../support/test-providers';
import { PublisherDetails } from '../../../../src/pages/admin-dashboard/publisher-details';
import { ExtensionRegistryService, AdminService } from '../../../../src/extension-registry-service';
import { PublisherInfo, UserData, UserRelationships } from '../../../../src/extension-registry-types';

function entry(): UserRelationships {
    return {
        user: { loginName: 'octocat', tokensUrl: '', createTokenUrl: '', provider: 'github' },
        namespaces: []
    };
}

function serviceReturning(publisherAgreement?: UserData['publisherAgreement']): ExtensionRegistryService {
    const publisherInfo: PublisherInfo = {
        user: { loginName: 'octocat', tokensUrl: '', createTokenUrl: '', provider: 'github', publisherAgreement },
        extensions: [],
        activeAccessTokenNum: 0
    };
    return {
        serverUrl: 'https://open-vsx.org',
        admin: { getPublisherInfo: () => Promise.resolve(publisherInfo) } as unknown as AdminService
    } as ExtensionRegistryService;
}

describe('PublisherDetails — publisher agreement chip', () => {
    // Regression: the backend falls back to 'none' whenever it cannot confirm the agreement
    // status (e.g. a blocked Eclipse profile) rather than reporting a status the frontend has
    // no rendering for, so 'none' is the only "nothing to show" state this chip needs to cover.
    it('renders the "Not signed" chip when the agreement status is "none"', async () => {
        renderWithProviders(<PublisherDetails entry={entry()} />, {
            mainContext: { service: serviceReturning({ status: 'none' }) }
        });

        expect(await screen.findByText('Publisher agreement: Not signed')).toBeInTheDocument();
    });
});
