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
import { useNavigate } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { MainContext } from '../../../context';
import { controllerFromSignal, NO_CACHE } from '../../../query-client';
import { createRoute } from '../../../utils';
import { UserSettingsRoutes } from '../user-settings-routes';

const NAMESPACES_QUERY_KEY = ['user', 'namespaces'];

/**
 * Loads the namespaces the current user is a member of. Shared by the settings
 * sidebar and the namespaces tab, which subscribe to the same cache entry.
 */
export const useUserNamespaces = () => {
    const { service, user } = useContext(MainContext);
    return useQuery({
        queryKey: NAMESPACES_QUERY_KEY,
        queryFn: ({ signal }) => service.getNamespaces(controllerFromSignal(signal)),
        enabled: user != null,
        ...NO_CACHE
    });
};

/** Returns a callback that re-fetches the namespace list, e.g. after creating a namespace. */
export const useRefreshUserNamespaces = () => {
    const queryClient = useQueryClient();
    return () => queryClient.invalidateQueries({ queryKey: NAMESPACES_QUERY_KEY });
};

/** `CreateNamespaceDialog` callback that refreshes the list and opens the new namespace. */
export const useHandleNamespaceCreated = () => {
    const refreshNamespaces = useRefreshUserNamespaces();
    const navigate = useNavigate();
    return (name: string) => {
        refreshNamespaces();
        navigate(createRoute([UserSettingsRoutes.NAMESPACES, name]));
    };
};
