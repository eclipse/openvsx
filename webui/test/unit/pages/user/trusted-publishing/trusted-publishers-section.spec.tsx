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
import { renderWithProviders } from '../../../support/test-providers';
import {
    ExtensionTrustedPublishers,
    UserNamespaceTrustedPublishers
} from '../../../../../src/pages/user/trusted-publishing/trusted-publishers-section';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';
import { PageSettings } from '../../../../../src/page-settings';
import { Namespace, TrustedPublisher, TrustedPublisherStatus } from '../../../../../src/extension-registry-types';
import { enabledStatus, testUser, trustedPublisher } from '../../../support/trusted-publishing';

// the section reads pageSettings.urls (docs link), which the harness default lacks
const pageSettings = { urls: {} } as PageSettings;

// `publishers` is a factory so a rejection is only created once the query consumes it.
function serviceStub(status: TrustedPublisherStatus, publishers: () => Promise<TrustedPublisher[]>) {
    const getTrustedPublishingStatus = vi.fn().mockResolvedValue(status);
    const getTrustedPublishers = vi.fn().mockImplementation(publishers);
    const service = {
        userTrustedPublishingUrl: (namespace: string) => `/user/namespace/${namespace}/trusted-publishing`,
        getTrustedPublishingStatus,
        getTrustedPublishers
    } as unknown as ExtensionRegistryService;
    return { getTrustedPublishingStatus, getTrustedPublishers, service };
}

describe('ExtensionTrustedPublishers', () => {
    it('renders the section when providers exist and the user may manage the namespace', async () => {
        const { service } = serviceStub(enabledStatus, () => Promise.resolve([trustedPublisher()]));
        renderWithProviders(<ExtensionTrustedPublishers namespace='foo' extension='bar' />, {
            mainContext: { service, user: testUser, pageSettings }
        });

        expect(await screen.findByText('Trusted Publishers')).toBeInTheDocument();
    });

    // the list endpoint is namespace-scoped, so extension pages filter client-side
    it('shows only the publishers of this extension', async () => {
        const publishers = [
            trustedPublisher({ id: 1, extension: 'bar', registration: { owner: 'octo', repo: 'one' } }),
            trustedPublisher({ id: 2, extension: 'other', registration: { owner: 'octo', repo: 'two' } })
        ];
        const { service } = serviceStub(enabledStatus, () => Promise.resolve(publishers));
        renderWithProviders(<ExtensionTrustedPublishers namespace='foo' extension='bar' />, {
            mainContext: { service, user: testUser, pageSettings }
        });

        expect(await screen.findByText('octo/one')).toBeInTheDocument();
        expect(screen.queryByText('octo/two')).not.toBeInTheDocument();
    });

    // The trusted-publishing URL is built client-side here, so the publishers list is
    // the ownership probe: it 403s for non-owners (e.g. an admin inspecting a foreign
    // extension) and the section must stay hidden instead of surfacing the error.
    it('stays hidden when the publishers list is forbidden', async () => {
        const { getTrustedPublishers, service } = serviceStub(enabledStatus, () =>
            Promise.reject({ error: 'Forbidden', status: 403 })
        );
        renderWithProviders(<ExtensionTrustedPublishers namespace='foo' extension='bar' />, {
            mainContext: { service, user: testUser, pageSettings }
        });

        await waitFor(() => expect(getTrustedPublishers).toHaveBeenCalled());
        expect(screen.queryByText('Trusted Publishers')).not.toBeInTheDocument();
    });

    it('stays hidden without providers (feature off or user not allowed), without probing publishers', async () => {
        const { getTrustedPublishingStatus, getTrustedPublishers, service } = serviceStub(
            { enabled: true, allowed: false },
            () => Promise.resolve([trustedPublisher()])
        );
        renderWithProviders(<ExtensionTrustedPublishers namespace='foo' extension='bar' />, {
            mainContext: { service, user: testUser, pageSettings }
        });

        await waitFor(() => expect(getTrustedPublishingStatus).toHaveBeenCalled());
        expect(getTrustedPublishers).not.toHaveBeenCalled();
        expect(screen.queryByText('Trusted Publishers')).not.toBeInTheDocument();
    });
});

describe('UserNamespaceTrustedPublishers', () => {
    const namespace = (trustedPublishingUrl?: string): Namespace => ({
        name: 'foo',
        extensions: { bar: 'https://server/api/foo/bar' },
        verified: true,
        membersUrl: '',
        roleUrl: '',
        detailsUrl: '',
        trustedPublishingUrl
    });

    it('renders the section for a namespace the user may manage', async () => {
        const { service } = serviceStub(enabledStatus, () => Promise.resolve([trustedPublisher()]));
        renderWithProviders(
            <UserNamespaceTrustedPublishers namespace={namespace('/user/namespace/foo/trusted-publishing')} />,
            { mainContext: { service, user: testUser, pageSettings } }
        );

        expect(await screen.findByText('Trusted Publishers')).toBeInTheDocument();
    });

    // the server grants trustedPublishingUrl only to namespace owners
    it('stays hidden when the namespace has no trusted-publishing URL', async () => {
        const { getTrustedPublishingStatus, service } = serviceStub(enabledStatus, () => Promise.resolve([]));
        renderWithProviders(<UserNamespaceTrustedPublishers namespace={namespace(undefined)} />, {
            mainContext: { service, user: testUser, pageSettings }
        });

        await waitFor(() => expect(getTrustedPublishingStatus).toHaveBeenCalled());
        expect(screen.queryByText('Trusted Publishers')).not.toBeInTheDocument();
    });
});
