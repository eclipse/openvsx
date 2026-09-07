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
import { MainContext } from '../../../context';
import { controllerFromSignal } from '../../../query-client';

/**
 * Runs a search and reports where each result's score came from.
 * <p>
 * Only fetches once a term has been submitted: an empty query matches everything, and a listing of
 * everything in score order answers no question anybody asked.
 */
export const useSearchExplain = (query: string, size: number) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: ['admin', 'search-explain', query, size] as const,
        queryFn: ({ signal }) => service.admin.explainSearch(controllerFromSignal(signal), query, size),
        enabled: query.length > 0
    });
};
