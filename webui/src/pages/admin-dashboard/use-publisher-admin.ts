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
import { keepPreviousData, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { isError } from '../../extension-registry-types';
import { controllerFromSignal } from '../../query-client';

export type PublisherRole = 'admin' | 'privileged' | 'none';

const PUBLISHERS_PAGE_SIZE = 25;

export const publisherAdminKeys = {
    detail: (provider: string, login: string) => ['admin', 'publisher', provider, login] as const,
    list: (search: string, role: string) => ['admin', 'publishers', { search, role }] as const
};

// Shared prefix for the publisher write operations so a single `useIsMutating`
// can tell whether any of them (role change, revokes) is currently in flight.
export const publisherMutationKey = ['admin', 'publisher-mutation'] as const;

export const usePublisherInfo = (login: string, provider = 'github', enabled = true) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: publisherAdminKeys.detail(provider, login),
        queryFn: ({ signal }) => service.admin.getPublisherInfo(controllerFromSignal(signal), provider, login),
        enabled: enabled && !!login,
        staleTime: 0
    });
};

/**
 * Loads the searchable, role-filterable publisher list one page at a time.
 * `keepPreviousData` keeps the current rows on screen while a changed search or
 * role filter loads, matching the rest of the admin dashboard.
 */
export const useInfinitePublishers = (search: string, role: string) => {
    const { service } = useContext(MainContext);
    return useInfiniteQuery({
        queryKey: publisherAdminKeys.list(search, role),
        queryFn: ({ pageParam, signal }) =>
            service.admin.getUsers(controllerFromSignal(signal), {
                search: search || undefined,
                role: role || undefined,
                size: PUBLISHERS_PAGE_SIZE,
                page: pageParam
            }),
        initialPageParam: 0,
        getNextPageParam: lastPage => {
            const { number, totalPages } = lastPage.page;
            return number + 1 < totalPages ? number + 1 : undefined;
        },
        placeholderData: keepPreviousData
    });
};

/**
 * Updates a publisher's role and refreshes the publisher list on success so the
 * new role is reflected. Throws on an error result so the caller's catch path runs.
 */
export const useUpdatePublisherRole = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationKey: [...publisherMutationKey, 'role'],
        mutationFn: async ({ provider, login, role }: { provider: string; login: string; role: PublisherRole }) => {
            const result = await service.admin.updateUserRole(provider, login, role);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['admin', 'publishers'] });
        }
    });
};

/**
 * Revokes all contributions of a publisher. Throws on an error result so the
 * caller's catch path runs.
 */
export const useRevokePublisherContributions = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationKey: [...publisherMutationKey, 'revoke-contributions'],
        mutationFn: async ({ provider, login }: { provider: string; login: string }) => {
            const result = await service.admin.revokePublisherContributions(provider, login);
            if (isError(result)) {
                throw result;
            }
            return result;
        }
    });
};

/**
 * Revokes the access tokens of a publisher. Throws on an error result so the
 * caller's catch path runs.
 */
export const useRevokeAccessTokens = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationKey: [...publisherMutationKey, 'revoke-tokens'],
        mutationFn: async ({ provider, login }: { provider: string; login: string }) => {
            const result = await service.admin.revokeAccessTokens(provider, login);
            if (isError(result)) {
                throw result;
            }
            return result;
        }
    });
};
