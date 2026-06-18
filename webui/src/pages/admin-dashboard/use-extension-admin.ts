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
import { Extension, isError } from '../../extension-registry-types';
import { controllerFromSignal } from '../../query-client';

interface ExtensionTarget {
    namespace: string;
    extension: string;
}

interface DeleteExtensionRequest {
    namespace: string;
    extension: string;
    targetPlatformVersions?: object[];
}

/**
 * Looks up an extension for the admin search view. `staleTime: 0` keeps each
 * lookup fresh and `retry: false` lets a 404 surface immediately as "not found".
 * Pass `null` until the user triggers a (validated) search.
 */
export const useAdminExtension = (target: ExtensionTarget | null) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: ['admin', 'extension', target?.namespace ?? '', target?.extension ?? ''],
        queryFn: async ({ signal }) => {
            const result = await service.admin.getExtension(controllerFromSignal(signal), target!.namespace, target!.extension);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
        enabled: !!target,
        retry: false,
        staleTime: 0,
    });
};

/**
 * Deletes extension versions. Mirrors the previous behaviour of not throwing on
 * an error result; thrown (network/server) errors reject so the caller's catch
 * path runs.
 */
export const useDeleteExtension = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: (req: DeleteExtensionRequest) => service.admin.deleteExtensions(req),
    });
};

/**
 * Loads an extension icon as an object URL. Caching is disabled so a fresh URL
 * is created per mount and the consumer can revoke the previous one without
 * risking reuse of a revoked URL from the cache.
 */
export const useExtensionIcon = (extension: Extension) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: ['extension-icon', extension.namespace, extension.name, extension.version, extension.targetPlatform],
        queryFn: async ({ signal }) => {
            const icon = await service.getExtensionIcon(controllerFromSignal(signal), extension);
            // useQuery forbids `undefined`; normalise the "no icon" case to null.
            return icon ?? null;
        },
        gcTime: 0,
        staleTime: 0,
    });
};
