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

import { useContext, useEffect } from 'react';
import { MainContext } from '../context';

/**
 * Passes a TanStack Query result through untouched, reporting any query error
 * to the global error dialog:
 *
 *     const namespacesQuery = useReportedQuery(useUserNamespaces());
 *
 * Opt-in on purpose: mutations and queries with bespoke error handling
 * (e.g. 404 → redirect) keep calling `handleError` themselves.
 */
export const useReportedQuery = <Q extends { error: Error | null }>(query: Q): Q => {
    const { handleError } = useContext(MainContext);
    const { error } = query;
    useEffect(() => {
        if (error) {
            handleError(error);
        }
    }, [error, handleError]);
    return query;
};
