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
import { MainContext } from '../../../context';
import { type Customer, isError, type UserData } from '../../../extension-registry-types';
import { controllerFromSignal } from '../../../query-client';

export const customerKeys = {
    all: ['admin', 'customers'] as const,
    detail: (name: string) => ['admin', 'customers', name] as const,
    tokens: (name: string) => ['admin', 'customers', name, 'tokens'] as const,
    members: (name: string) => ['admin', 'customers', name, 'members'] as const
};

/**
 * Loads all customers.
 */
export const useCustomers = () => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: customerKeys.all,
        queryFn: ({ signal }) => service.admin.getCustomers(controllerFromSignal(signal))
    });
};

/**
 * Loads a single customer by name.
 */
export const useCustomer = (name: string | undefined) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: customerKeys.detail(name ?? ''),
        queryFn: ({ signal }) => service.admin.getCustomer(controllerFromSignal(signal), name!),
        enabled: !!name
    });
};

/**
 * Creates a customer and refreshes the customer list on success.
 */
export const useCreateCustomer = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (customer: Customer) => service.admin.createCustomer(customer),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.all });
        }
    });
};

/**
 * Updates a customer and refreshes both the list and the affected detail view.
 */
export const useUpdateCustomer = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ name, customer }: { name: string; customer: Customer }) =>
            service.admin.updateCustomer(name, customer),
        onSuccess: (_data, { name }) => {
            queryClient.invalidateQueries({ queryKey: customerKeys.all });
            queryClient.invalidateQueries({ queryKey: customerKeys.detail(name) });
        }
    });
};

/**
 * Deletes a customer and refreshes the customer list on success.
 */
export const useDeleteCustomer = () => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (name: string) => service.admin.deleteCustomer(name),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.all });
        }
    });
};

/**
 * Loads the rate-limit tokens for a customer.
 */
export const useCustomerTokens = (name: string) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: customerKeys.tokens(name),
        queryFn: ({ signal }) => service.admin.getCustomerRateLimitTokens(controllerFromSignal(signal), name)
    });
};

/**
 * Generates a rate-limit token for a customer and refreshes the token list.
 */
export const useCreateCustomerToken = (name: string) => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (description: string) => service.admin.createCustomerRateLimitToken(name, description),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.tokens(name) });
        }
    });
};

/**
 * Deletes a rate-limit token for a customer and refreshes the token list.
 */
export const useDeleteCustomerToken = (name: string) => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (tokenId: number) => {
            const result = await service.admin.deleteCustomerRateLimitToken(name, tokenId);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.tokens(name) });
        }
    });
};

/**
 * Loads the members assigned to a customer.
 */
export const useCustomerMembers = (name: string) => {
    const { service } = useContext(MainContext);
    return useQuery({
        queryKey: customerKeys.members(name),
        queryFn: ({ signal }) => service.admin.getCustomerMembers(controllerFromSignal(signal), name)
    });
};

/**
 * Adds a member to a customer and refreshes the member list on success.
 */
export const useAddCustomerMember = (name: string) => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (user: UserData) => {
            const result = await service.admin.addCustomerMember(name, user);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.members(name) });
        }
    });
};

/**
 * Removes a member from a customer and refreshes the member list on success.
 */
export const useRemoveCustomerMember = (name: string) => {
    const { service } = useContext(MainContext);
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (user: UserData) => {
            const result = await service.admin.removeCustomerMember(name, user);
            if (isError(result)) {
                throw result;
            }
            return result;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: customerKeys.members(name) });
        }
    });
};
