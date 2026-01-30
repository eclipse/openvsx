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

/**
 * Hook for managing scan filter state and actions.
 * Provides access to global filters (date range, enforcement) and
 * tab-specific filters (status, quarantine, threat scanner, validation type).
 */
export const useScanFilters = () => {
    const { state, actions } = useScanContext();

    // Global filters
    const globalFilters = useMemo(() => ({
        dateRange: state.dateRange,
        enforcement: state.enforcement,
        setDateRange: actions.setDateRange,
        setEnforcement: actions.setEnforcement,
    }), [state.dateRange, state.enforcement, actions]);

    // Status filters (for Scans tab)
    const statusFilters = useMemo(() => ({
        filters: state.statusFilters,
        toggle: actions.toggleStatusFilter,
        menuAnchor: state.filterMenuAnchor,
        openMenu: actions.openFilterMenu,
        closeMenu: actions.closeFilterMenu,
    }), [state.statusFilters, state.filterMenuAnchor, actions]);

    // Quarantine filters (for Quarantined tab)
    const quarantineFilters = useMemo(() => ({
        filters: state.quarantineFilters,
        toggle: actions.toggleQuarantineFilter,
        threatScannerFilters: state.threatScannerFilters,
        toggleThreatScanner: actions.toggleThreatScannerFilter,
        availableThreatScanners: state.availableThreatScanners,
        menuAnchor: state.quarantineFilterMenuAnchor,
        openMenu: actions.openQuarantineFilterMenu,
        closeMenu: actions.closeQuarantineFilterMenu,
    }), [
        state.quarantineFilters,
        state.threatScannerFilters,
        state.availableThreatScanners,
        state.quarantineFilterMenuAnchor,
        actions,
    ]);

    // Validation type filters (for Auto Rejected tab)
    const validationTypeFilters = useMemo(() => ({
        filters: state.validationTypeFilters,
        toggle: actions.toggleValidationTypeFilter,
        availableValidationTypes: state.availableValidationTypes,
        menuAnchor: state.autoRejectedFilterMenuAnchor,
        openMenu: actions.openAutoRejectedFilterMenu,
        closeMenu: actions.closeAutoRejectedFilterMenu,
    }), [
        state.validationTypeFilters,
        state.availableValidationTypes,
        state.autoRejectedFilterMenuAnchor,
        actions,
    ]);

    return {
        globalFilters,
        statusFilters,
        quarantineFilters,
        validationTypeFilters,
    };
};

export type UseScanFiltersReturn = ReturnType<typeof useScanFilters>;
