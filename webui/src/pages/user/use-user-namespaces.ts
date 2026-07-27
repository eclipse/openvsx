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
import { useQuery } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { controllerFromSignal } from '../../query-client';

/** The namespaces the current user is a member of. */
export const useUserNamespaces = () => {
    const { service, user } = useContext(MainContext);
    return useQuery({
        queryKey: ['user', 'namespaces'],
        queryFn: ({ signal }) => service.getNamespaces(controllerFromSignal(signal)),
        enabled: user != null,
        // TODO: getNamespaces still goes through the retriable sendRequest (shared with an imperative
        // caller), so disable TanStack retries to avoid double-retrying until it's migrated to
        // sendNonRetriableRequest.
        retry: false
    });
};
