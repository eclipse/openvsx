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
import { act, waitFor } from '@testing-library/react';
import { useLocation } from 'react-router';
import { renderHookWithProviders } from '../../../support/test-providers';
import { testUser } from '../../../support/trusted-publishing';
import { settingsServiceStub, testNamespace } from '../../../support/user-settings';
import {
    useHandleNamespaceCreated,
    useUserNamespaces
} from '../../../../../src/pages/user/namespaces/use-user-namespaces';

describe('useUserNamespaces', () => {
    it('idles without a logged-in user', () => {
        const { getNamespaces, service } = settingsServiceStub();
        renderHookWithProviders(() => useUserNamespaces(), { mainContext: { service } });

        expect(getNamespaces).not.toHaveBeenCalled();
    });

    it('loads the namespaces of a logged-in user', async () => {
        const { service } = settingsServiceStub({ namespaces: [testNamespace({ name: 'redhat' })] });
        const { result } = renderHookWithProviders(() => useUserNamespaces(), {
            mainContext: { service, user: testUser }
        });

        await waitFor(() => expect(result.current.data?.map(ns => ns.name)).toEqual(['redhat']));
    });
});

describe('useHandleNamespaceCreated', () => {
    it('refreshes the namespace list and opens the new namespace', async () => {
        const { getNamespaces, service } = settingsServiceStub({ namespaces: [testNamespace({ name: 'redhat' })] });
        const { result } = renderHookWithProviders(
            () => ({
                // subscribed so the invalidation below has a live query to refetch
                namespaces: useUserNamespaces(),
                handleCreated: useHandleNamespaceCreated(),
                location: useLocation()
            }),
            { mainContext: { service, user: testUser } }
        );
        await waitFor(() => expect(getNamespaces).toHaveBeenCalledTimes(1));

        act(() => result.current.handleCreated('acme'));

        await waitFor(() => expect(result.current.location.pathname).toBe('/user-settings/namespaces/acme'));
        await waitFor(() => expect(getNamespaces).toHaveBeenCalledTimes(2));
    });
});
