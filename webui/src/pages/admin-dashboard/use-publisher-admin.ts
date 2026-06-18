/******************************************************************************
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
 *****************************************************************************/

import { useContext } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { isError } from '../../extension-registry-types';
import { controllerFromSignal } from '../../query-client';

export const publisherAdminKeys = {
    detail: (provider: string, login: string) => ['admin', 'publisher', provider, login] as const,
};

/**
 * Looks up publisher info for the admin search view. `staleTime: 0` keeps each
 * lookup fresh and `retry: false` lets a 404 surface immediately as "not found".
 */
export const usePublisherInfo = (login: string, provider = 'github') => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: publisherAdminKeys.detail(provider, login),
        queryFn: ({ signal }) => service.admin.getPublisherInfo(controllerFromSignal(signal), provider, login),
        enabled: !!login,
        retry: false,
        staleTime: 0,
    });
};

/**
 * Revokes all contributions of a publisher. Throws on an error result so the
 * caller's catch path runs.
 */
export const useRevokePublisherContributions = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: async ({ provider, login }: { provider: string; login: string }) => {
            const result = await service.admin.revokePublisherContributions(provider, login);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
    });
};

/**
 * Revokes the access tokens of a publisher. Throws on an error result so the
 * caller's catch path runs.
 */
export const useRevokeAccessTokens = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: async ({ provider, login }: { provider: string; login: string }) => {
            const result = await service.admin.revokeAccessTokens(provider, login);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
    });
};
