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

import { useMemo } from 'react';
import { useScanContext } from '../../context/scan-admin';
import { useScanFilters } from './use-scan-filters';
import { usePagination } from './use-pagination';
import { useSearch } from './use-search';

/**
 * Hook for the main Scans tab (tab index 0).
 * Provides all data and actions needed for displaying scan overview.
 */
export const useScansTab = () => {
    const { state, actions } = useScanContext();
    const { globalFilters, statusFilters } = useScanFilters();
    const pagination = usePagination();
    const search = useSearch();

    // Get scan counts for display
    const counts = useMemo(() => {
        if (!state.scanCounts) {
            return {
                started: 0,
                validating: 0,
                scanning: 0,
                passed: 0,
                quarantined: 0,
                autoRejected: 0,
                error: 0,
                allowed: 0,
                blocked: 0,
                needsReview: 0,
                total: 0,
            };
        }
        return {
            started: state.scanCounts.STARTED,
            validating: state.scanCounts.VALIDATING,
            scanning: state.scanCounts.SCANNING,
            passed: state.scanCounts.PASSED,
            quarantined: state.scanCounts.QUARANTINED,
            autoRejected: state.scanCounts.AUTO_REJECTED,
            error: state.scanCounts.ERROR,
            allowed: state.scanCounts.ALLOWED,
            blocked: state.scanCounts.BLOCKED,
            needsReview: state.scanCounts.NEEDS_REVIEW,
            // Total = sum of scan statuses only (ALLOWED/BLOCKED/NEEDS_REVIEW are admin decisions, not statuses)
            total: state.scanCounts.STARTED + state.scanCounts.VALIDATING + state.scanCounts.SCANNING +
                   state.scanCounts.PASSED + state.scanCounts.QUARANTINED + state.scanCounts.AUTO_REJECTED +
                   state.scanCounts.ERROR,
        };
    }, [state.scanCounts]);

    return useMemo(() => ({
        tabIndex: 0,
        tabName: 'Scans',
        scans: state.scans,
        isLoading: state.isLoadingScans,
        lastRefreshed: state.lastRefreshed,
        autoRefresh: state.autoRefresh,
        onAutoRefreshChange: actions.setAutoRefresh,
        counts,
        search,
        globalFilters,
        statusFilters,
        pagination,
    }), [
        state.scans,
        state.isLoadingScans,
        state.lastRefreshed,
        state.autoRefresh,
        actions.setAutoRefresh,
        counts,
        search,
        globalFilters,
        statusFilters,
        pagination,
    ]);
};

export type UseScansTabReturn = ReturnType<typeof useScansTab>;
