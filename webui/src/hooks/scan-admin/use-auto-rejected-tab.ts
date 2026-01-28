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
 * Hook for the Auto Rejected tab (tab index 2).
 * Provides auto-rejected scan data and validation type filtering.
 */
export const useAutoRejectedTab = () => {
    const { state, actions } = useScanContext();
    const { globalFilters, validationTypeFilters } = useScanFilters();
    const pagination = usePagination();
    const search = useSearch();

    // Use scans directly from state - backend handles filtering based on tab and enforcement
    const autoRejectedScans = state.scans;

    // Get validation type breakdown
    const validationTypeBreakdown = useMemo(() => {
        const breakdown: Record<string, number> = {};
        autoRejectedScans.forEach(scan => {
            scan.validationFailures?.forEach(failure => {
                const type = failure.type || 'Unknown';
                breakdown[type] = (breakdown[type] || 0) + 1;
            });
        });
        return breakdown;
    }, [autoRejectedScans]);

    // Get total count from the counts API (unaffected by search filters)
    const totalCount = (state.scanCounts?.STARTED ?? 0) + (state.scanCounts?.VALIDATING ?? 0) +
        (state.scanCounts?.SCANNING ?? 0) + (state.scanCounts?.PASSED ?? 0) +
        (state.scanCounts?.QUARANTINED ?? 0) + (state.scanCounts?.AUTO_REJECTED ?? 0) +
        (state.scanCounts?.ERROR ?? 0);

    // Check if there are any validation types available to filter by
    const hasValidationTypes = state.availableValidationTypes.length > 0;

    return useMemo(() => ({
        tabIndex: 2,
        tabName: 'Auto Rejected',
        scans: autoRejectedScans,
        isLoading: state.isLoadingScans,
        lastRefreshed: state.lastRefreshed,
        autoRefresh: state.autoRefresh,
        onAutoRefreshChange: actions.setAutoRefresh,
        totalCount,
        hasValidationTypes,
        search,
        globalFilters,
        validationTypeFilters,
        pagination,
        validationTypeBreakdown,
    }), [
        autoRejectedScans,
        state.isLoadingScans,
        state.lastRefreshed,
        state.autoRefresh,
        actions.setAutoRefresh,
        totalCount,
        hasValidationTypes,
        search,
        globalFilters,
        validationTypeFilters,
        pagination,
        validationTypeBreakdown,
    ]);
};

export type UseAutoRejectedTabReturn = ReturnType<typeof useAutoRejectedTab>;
