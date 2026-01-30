/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { useMemo, useCallback } from 'react';
import { useScanContext } from '../../context/scan-admin';

/**
 * Hook for managing search state and actions.
 * Provides publisher, namespace, and name search functionality.
 */
export const useSearch = () => {
    const { state, actions, dispatch } = useScanContext();

    const clearSearch = useCallback(() => {
        dispatch({ type: 'CLEAR_SEARCH' });
    }, [dispatch]);

    const hasActiveSearch = useMemo(() => {
        return !!(state.publisherQuery || state.namespaceQuery || state.nameQuery);
    }, [state.publisherQuery, state.namespaceQuery, state.nameQuery]);

    return useMemo(() => ({
        publisherQuery: state.publisherQuery,
        namespaceQuery: state.namespaceQuery,
        nameQuery: state.nameQuery,
        setPublisherQuery: actions.setPublisherQuery,
        setNamespaceQuery: actions.setNamespaceQuery,
        setNameQuery: actions.setNameQuery,
        handlePublisherChange: actions.handlePublisherChange,
        handleNamespaceChange: actions.handleNamespaceChange,
        handleNameChange: actions.handleNameChange,
        clearSearch,
        hasActiveSearch,
    }), [
        state.publisherQuery,
        state.namespaceQuery,
        state.nameQuery,
        actions,
        clearSearch,
        hasActiveSearch,
    ]);
};

export type UseSearchReturn = ReturnType<typeof useSearch>;
