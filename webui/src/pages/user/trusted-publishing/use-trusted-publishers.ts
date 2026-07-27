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
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { MainContext } from '../../../context';
import {
    TrustedPublisher,
    TrustedPublisherProvider,
    TrustedPublisherRequest,
    UrlString
} from '../../../extension-registry-types';
import { controllerFromSignal } from '../../../query-client';

// keyed by the namespace's trusted-publishing URL (the endpoint we actually query)
export const trustedPublisherKeys = {
    providers: (trustedPublishingUrl?: UrlString) =>
        ['user', 'trusted-publisher-providers', trustedPublishingUrl] as const,
    publishers: (trustedPublishingUrl?: UrlString) => ['user', 'trusted-publishers', trustedPublishingUrl] as const
};

/**
 * Fails when trusted publishing is disabled server-side or the current user
 * cannot manage the namespace, so an error means "feature unavailable". Idle
 * when there is no URL, since that already means the user cannot manage it.
 */
export const useTrustedPublisherProviders = (trustedPublishingUrl?: UrlString) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: trustedPublisherKeys.providers(trustedPublishingUrl),
        queryFn: ({ signal }) =>
            service.getTrustedPublisherProviders(controllerFromSignal(signal), trustedPublishingUrl!),
        enabled: Boolean(trustedPublishingUrl)
    });
};

/** Providers of several namespaces at once, keyed by trusted-publishing URL; failed probes yield empty lists. */
export const useAllTrustedPublisherProviders = (trustedPublishingUrls: UrlString[]) => {
    const { service } = useContext(MainContext);
    return useQueries({
        queries: trustedPublishingUrls.map(trustedPublishingUrl => ({
            queryKey: trustedPublisherKeys.providers(trustedPublishingUrl),
            queryFn: ({ signal }: { signal?: AbortSignal }) =>
                service.getTrustedPublisherProviders(controllerFromSignal(signal), trustedPublishingUrl)
        })),
        combine: results => ({
            isLoading: results.some(result => result.isLoading),
            providersByUrl: Object.fromEntries(
                trustedPublishingUrls.map((trustedPublishingUrl, index) => [
                    trustedPublishingUrl,
                    results[index]?.data ?? []
                ])
            ) as Record<string, readonly TrustedPublisherProvider[]>
        })
    });
};

export const useTrustedPublishers = (trustedPublishingUrl?: UrlString) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: trustedPublisherKeys.publishers(trustedPublishingUrl),
        queryFn: ({ signal }) => service.getTrustedPublishers(controllerFromSignal(signal), trustedPublishingUrl!),
        enabled: Boolean(trustedPublishingUrl)
    });
};

/** The publishers of several namespaces flattened into one list. */
export const useAllTrustedPublishers = (trustedPublishingUrls: UrlString[]) => {
    const { service } = useContext(MainContext);
    return useQueries({
        queries: trustedPublishingUrls.map(trustedPublishingUrl => ({
            queryKey: trustedPublisherKeys.publishers(trustedPublishingUrl),
            queryFn: ({ signal }: { signal?: AbortSignal }) =>
                service.getTrustedPublishers(controllerFromSignal(signal), trustedPublishingUrl)
        })),
        combine: results => ({
            isLoading: results.some(result => result.isLoading),
            publishers: results.flatMap(result => result.data ?? []) as TrustedPublisher[]
        })
    });
};

export const useRegisterTrustedPublisher = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({
            trustedPublishingUrl,
            request
        }: {
            trustedPublishingUrl: UrlString;
            request: TrustedPublisherRequest;
        }) => service.registerTrustedPublisher(trustedPublishingUrl, request),
        onSuccess: (_result, { trustedPublishingUrl }) => {
            queryClient.invalidateQueries({ queryKey: trustedPublisherKeys.publishers(trustedPublishingUrl) });
        }
    });
};

export const useDeleteTrustedPublisher = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ trustedPublishingUrl, id }: { trustedPublishingUrl: UrlString; id: number }) =>
            service.deleteTrustedPublisher(trustedPublishingUrl, id),
        onSuccess: (_result, { trustedPublishingUrl }) => {
            queryClient.invalidateQueries({ queryKey: trustedPublisherKeys.publishers(trustedPublishingUrl) });
        }
    });
};
