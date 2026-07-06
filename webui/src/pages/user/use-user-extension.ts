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

interface UserExtensionTarget {
    namespace: string;
    extension: string;
}

interface DeleteUserExtensionRequest {
    namespace: string;
    extension: string;
    targetPlatformVersions?: object[];
}

/**
 * Loads one of the current user's extensions for the extension settings page.
 */
export const useUserExtension = (target: UserExtensionTarget) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: ['user', 'extension', target.namespace, target.extension],
        queryFn: async ({ signal }) => {
            const result = await service.getExtension(controllerFromSignal(signal), target.namespace, target.extension);
            if (isError(result)) {
                throw result;
            }
            return result;
        }
    });
};

/**
 * Deletes extension versions. Mirrors the previous behaviour of not throwing on
 * an error result; thrown (network/server) errors reject so the caller's catch
 * path runs.
 */
export const useDeleteUserExtensionVersions = () => {
    const { service } = useContext(MainContext);
    return useMutation({
        mutationFn: (req: DeleteUserExtensionRequest) => service.deleteExtensions(req)
    });
};
