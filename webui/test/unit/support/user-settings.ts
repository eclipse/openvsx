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
    Customer,
    EnforcementState,
    Namespace,
    NamespaceDetails,
    TrustedPublisherStatus
} from '../../../src/extension-registry-types';
import { enabledStatus, statusServiceStub } from './trusted-publishing';

/** A namespace carrying the capability URLs the settings views read. */
export const testNamespace = (overrides: Partial<Namespace> = {}): Namespace => ({
    name: 'foo',
    extensions: {},
    verified: true,
    membersUrl: '',
    roleUrl: '',
    detailsUrl: '',
    ...overrides
});

/** A namespace's public details, as the details form and the logo sidebar read them. */
export const namespaceDetails = (overrides: Partial<NamespaceDetails> = {}): NamespaceDetails => ({
    name: 'foo',
    displayName: 'Foo',
    socialLinks: {},
    ...overrides
});

/** A rate-limiting customer group the user can be a member of. */
export const testCustomer = (overrides: Partial<Customer> = {}): Customer => ({
    name: 'acme',
    state: EnforcementState.EVALUATION,
    cidrBlocks: [],
    ...overrides
});

export interface SettingsStubData {
    status?: TrustedPublisherStatus;
    customers?: Customer[];
    namespaces?: Namespace[];
}

/**
 * Service stub answering every query the settings navigation makes: the trusted-publishing
 * status and the customer-group and namespace lists that gate the tabs and fill the sidebar.
 */
export function settingsServiceStub({
    status = enabledStatus,
    customers = [],
    namespaces = []
}: SettingsStubData = {}) {
    const { getTrustedPublishingStatus, service } = statusServiceStub(status);
    const getCustomers = vi.fn().mockResolvedValue(customers);
    const getNamespaces = vi.fn().mockResolvedValue(namespaces);
    return {
        getTrustedPublishingStatus,
        getCustomers,
        getNamespaces,
        service: { ...service, getCustomers, getNamespaces } as unknown as ExtensionRegistryService
    };
}
