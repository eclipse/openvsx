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

import { useContext, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { controllerFromSignal } from '../../query-client';
import { Extension, SearchEntry } from '../../extension-registry-types';

/**
 * Loads an extension's icon as an object URL, returned as the react-query result
 * (`data` is `null` when the extension has no icon). Caching is disabled
 * (`gcTime`/`staleTime` 0) so each mount fetches a fresh URL and never reuses an
 * already-revoked one; the previous URL is revoked when replaced and on unmount.
 */
export const useExtensionIcon = (extension: Extension | SearchEntry) => {
    const { service } = useContext(MainContext);
    const targetPlatform = 'targetPlatform' in extension ? extension.targetPlatform : undefined;
    const query = useQuery({
        queryKey: [
            'extension-icon',
            extension.namespace,
            extension.name,
            extension.version,
            targetPlatform,
            extension.files.icon
        ],
        queryFn: async ({ signal }) => {
            const icon = await service.getExtensionIcon(controllerFromSignal(signal), extension);
            // useQuery forbids `undefined`; normalise the "no icon" case to null.
            return icon ?? null;
        },
        gcTime: 0,
        staleTime: 0
    });

    const previousUrl = useRef<string | null>(null);
    useEffect(() => {
        if (previousUrl.current && previousUrl.current !== query.data) {
            URL.revokeObjectURL(previousUrl.current);
        }
        previousUrl.current = query.data ?? null;
    }, [query.data]);

    useEffect(
        () => () => {
            if (previousUrl.current) {
                URL.revokeObjectURL(previousUrl.current);
            }
        },
        []
    );

    return query;
};
