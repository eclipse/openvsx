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
import { PublisherRevokeContributionsButton } from '../../../../src/pages/admin-dashboard/publisher-revoke-dialog';
import { ExtensionRegistryService, AdminService } from '../../../../src/extension-registry-service';
import { PublisherInfo, UserData } from '../../../../src/extension-registry-types';

const service = { serverUrl: 'https://open-vsx.org', admin: {} as AdminService } as ExtensionRegistryService;

function publisherInfo(publisherAgreement?: UserData['publisherAgreement']): PublisherInfo {
    return {
        user: { loginName: 'octocat', tokensUrl: '', createTokenUrl: '', provider: 'github', publisherAgreement },
        extensions: [],
        activeAccessTokenNum: 0
    };
}

const eclipseLoginButton = () => screen.queryByRole('link', { name: /log in with eclipse/i });
const revokeButton = () => screen.getByRole('button', { name: /revoke publisher contributions/i });

describe('PublisherRevokeContributionsButton', () => {
    it('prompts the admin to log in with Eclipse when the publisher has an agreement to revoke', () => {
        renderWithProviders(
            <PublisherRevokeContributionsButton publisherInfo={publisherInfo({ status: 'signed' })} />,
            { mainContext: { service } }
        );

        expect(eclipseLoginButton()).toBeInTheDocument();
    });

    // Regression test: the gate used to check for the mere presence of a
    // publisherAgreement object, which the backend also sends with status
    // 'none' — forcing an unneeded Eclipse login even though there is nothing
    // to revoke on the agreement side.
    it('does not require an Eclipse login when the publisher agreement status is "none"', () => {
        renderWithProviders(<PublisherRevokeContributionsButton publisherInfo={publisherInfo({ status: 'none' })} />, {
            mainContext: { service }
        });

        expect(eclipseLoginButton()).not.toBeInTheDocument();
        expect(revokeButton()).toBeInTheDocument();
    });

    // Regression: a missing publisherAgreement (the backend could not reliably determine the
    // status) used to read as "has an agreement" - `undefined !== 'none'` is true - forcing an
    // unneeded Eclipse login even though there is nothing confirmed to revoke.
    it('does not require an Eclipse login when the publisher agreement status could not be determined', () => {
        renderWithProviders(<PublisherRevokeContributionsButton publisherInfo={publisherInfo(undefined)} />, {
            mainContext: { service }
        });

        expect(eclipseLoginButton()).not.toBeInTheDocument();
        expect(revokeButton()).toBeInTheDocument();
    });

    it('does not prompt for an Eclipse login once the admin is already logged in with Eclipse', () => {
        renderWithProviders(
            <PublisherRevokeContributionsButton publisherInfo={publisherInfo({ status: 'signed' })} />,
            {
                mainContext: {
                    service,
                    user: {
                        loginName: 'admin',
                        tokensUrl: '',
                        createTokenUrl: '',
                        additionalLogins: [
                            { loginName: 'admin', tokensUrl: '', createTokenUrl: '', provider: 'eclipse' }
                        ]
                    }
                }
            }
        );

        expect(eclipseLoginButton()).not.toBeInTheDocument();
        expect(revokeButton()).toBeInTheDocument();
    });
});
