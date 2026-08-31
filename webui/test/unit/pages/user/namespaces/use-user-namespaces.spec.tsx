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
import { act, waitFor } from '@testing-library/react';
import { useLocation } from 'react-router';
import { renderHookWithProviders } from '../../../support/test-providers';
import { testUser } from '../../../support/trusted-publishing';
import { settingsServiceStub, testNamespace } from '../../../support/user-settings';
import {
    useCreateNamespace,
    useHandleNamespaceCreated,
    useUserNamespaces
} from '../../../../../src/pages/user/namespaces/use-user-namespaces';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';

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

describe('useCreateNamespace', () => {
    function renderCreate(createNamespace: ReturnType<typeof vi.fn>) {
        const { getNamespaces, service } = settingsServiceStub({ namespaces: [testNamespace({ name: 'redhat' })] });
        return {
            getNamespaces,
            ...renderHookWithProviders(
                () => ({
                    // subscribed so the invalidation below has a live query to refetch
                    namespaces: useUserNamespaces(),
                    create: useCreateNamespace()
                }),
                {
                    mainContext: {
                        service: { ...service, createNamespace } as unknown as ExtensionRegistryService,
                        user: testUser
                    }
                }
            )
        };
    }

    it('re-reads the namespace list once the namespace exists', async () => {
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        const { getNamespaces, result } = renderCreate(createNamespace);
        await waitFor(() => expect(getNamespaces).toHaveBeenCalledTimes(1));

        await act(async () => {
            await result.current.create.mutateAsync('acme');
        });

        expect(createNamespace).toHaveBeenCalledWith('acme');
        await waitFor(() => expect(getNamespaces).toHaveBeenCalledTimes(2));
    });

    it('rejects when the registry turns the name down, leaving the list alone', async () => {
        const createNamespace = vi.fn().mockRejectedValue({ error: 'Namespace already exists' });
        const { getNamespaces, result } = renderCreate(createNamespace);
        await waitFor(() => expect(getNamespaces).toHaveBeenCalledTimes(1));

        await expect(result.current.create.mutateAsync('redhat')).rejects.toMatchObject({
            error: 'Namespace already exists'
        });
        expect(getNamespaces).toHaveBeenCalledTimes(1);
    });
});

describe('useHandleNamespaceCreated', () => {
    it('opens the namespace just created', async () => {
        const { service } = settingsServiceStub();
        const { result } = renderHookWithProviders(
            () => ({ handleCreated: useHandleNamespaceCreated(), location: useLocation() }),
            { mainContext: { service, user: testUser } }
        );

        act(() => result.current.handleCreated('acme'));

        await waitFor(() => expect(result.current.location.pathname).toBe('/user-settings/namespaces/acme'));
    });
});
