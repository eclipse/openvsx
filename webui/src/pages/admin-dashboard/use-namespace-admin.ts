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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { controllerFromSignal } from '../../query-client';

interface ChangeNamespaceRequest {
    oldNamespace: string;
    newNamespace: string;
    removeOldNamespace: boolean;
    mergeIfNewNamespaceAlreadyExists: boolean;
}

export const namespaceAdminKeys = {
    detail: (name: string) => ['admin', 'namespace', name] as const
};

/**
 * Looks up a single namespace for the admin search view. `staleTime: 0` keeps
 * each lookup fresh and `retry: false` lets a 404 surface immediately as a
 * "not found" result rather than being retried.
 */
export const useAdminNamespace = (name: string) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: namespaceAdminKeys.detail(name),
        queryFn: ({ signal }) => service.admin.getNamespace(controllerFromSignal(signal), name),
        enabled: !!name,
        retry: false,
        staleTime: 0
    });
};

/**
 * Creates a namespace and refreshes the matching lookup on success.
 */
export const useCreateNamespace = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (name: string) => service.admin.createNamespace({ name }),
        onSuccess: (_result, name) => {
            queryClient.invalidateQueries({ queryKey: namespaceAdminKeys.detail(name) });
        }
    });
};

/**
 * Renames (and optionally merges) a namespace.
 */
export const useChangeNamespace = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: (req: ChangeNamespaceRequest) => service.admin.changeNamespace(req)
    });
};

/**
 * Deletes a namespace.
 */
export const useDeleteNamespace = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: (name: string) => service.admin.deleteNamespace({ name })
    });
};

/**
 * Drops a namespace lookup from the cache, e.g. after it has been deleted so a
 * later search refetches instead of showing the removed namespace.
 */
export const useClearAdminNamespace = () => {
    const queryClient = useQueryClient();
    return (name: string) => {
        queryClient.removeQueries({ queryKey: namespaceAdminKeys.detail(name) });
    };
};
