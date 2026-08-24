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

import { vi } from 'vitest';
import { ExtensionRegistryService } from '../../../src/extension-registry-service';
import {
    TrustedPublisher,
    TrustedPublisherList,
    TrustedPublisherProvider,
    TrustedPublisherStatus,
    UserData
} from '../../../src/extension-registry-types';

/** Minimal logged-in user for MainContext overrides. */
export const testUser = { loginName: 'test', tokensUrl: '', createTokenUrl: '' } as UserData;

/** A provider with the GitHub-style registration inputs (environment optional). */
export const gitHubProvider: TrustedPublisherProvider = {
    id: 'github',
    name: 'GitHub Actions',
    url: 'https://github.com',
    registrationInputs: [
        { key: 'owner', description: 'Repository owner', optional: false },
        { key: 'repo', description: 'Repository name', optional: false },
        { key: 'workflow', description: 'Workflow file', optional: false },
        { key: 'environment', description: 'Environment', optional: true }
    ]
};

export const enabledStatus: TrustedPublisherStatus = {
    enabled: true,
    allowed: true,
    trustedPublisherProviders: [gitHubProvider]
};

/** Feature off registry-wide: no providers, so every trusted-publishing surface stays hidden. */
export const disabledStatus: TrustedPublisherStatus = { enabled: false, allowed: false };

export const trustedPublisher = (overrides: Partial<TrustedPublisher> = {}): TrustedPublisher => ({
    id: 1,
    provider: 'github',
    namespace: 'foo',
    extension: 'bar',
    registration: {},
    ...overrides
});

/** A namespace's trusted-publishing list response: registrations plus what is still registrable. */
export const trustedPublisherList = (overrides: Partial<TrustedPublisherList> = {}): TrustedPublisherList => ({
    trustedPublishers: [],
    registrableExtensions: [],
    ...overrides
});

/** Service stub answering only the trusted-publishing status query. */
export function statusServiceStub(status: TrustedPublisherStatus = enabledStatus) {
    const getTrustedPublishingStatus = vi.fn().mockResolvedValue(status);
    return {
        getTrustedPublishingStatus,
        service: { getTrustedPublishingStatus } as unknown as ExtensionRegistryService
    };
}
