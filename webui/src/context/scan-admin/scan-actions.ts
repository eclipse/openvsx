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
import { ScanAction, DateRangeType, EnforcementType, FileActionType, ScanResult } from './scan-types';
import { ScanActions } from './scan-context-types';

// ============================================================================
// Actions Factory Hook
// ============================================================================

/**
 * Hook that creates memoized action creators for the scan context
 */
export const useScanActions = (
    dispatch: React.Dispatch<ScanAction>,
    executeConfirmAction: () => void,
    executeFileAction: () => void
): ScanActions => {
    return useMemo(() => ({
        // Tab
        setTab: (tab: number) => dispatch({ type: 'SET_TAB', payload: tab }),

        // Search
        setPublisherQuery: (query: string) => dispatch({ type: 'SET_PUBLISHER_QUERY', payload: query }),
        setNamespaceQuery: (query: string) => dispatch({ type: 'SET_NAMESPACE_QUERY', payload: query }),
        setNameQuery: (query: string) => dispatch({ type: 'SET_NAME_QUERY', payload: query }),
        handlePublisherChange: (event: React.ChangeEvent<HTMLInputElement>) =>
            dispatch({ type: 'SET_PUBLISHER_QUERY', payload: event.target.value }),
        handleNamespaceChange: (event: React.ChangeEvent<HTMLInputElement>) =>
            dispatch({ type: 'SET_NAMESPACE_QUERY', payload: event.target.value }),
        handleNameChange: (event: React.ChangeEvent<HTMLInputElement>) =>
            dispatch({ type: 'SET_NAME_QUERY', payload: event.target.value }),

        // Pagination
        setPage: (page: number) => dispatch({ type: 'SET_PAGE', payload: page }),

        // Filters
        setDateRange: (range: DateRangeType) => dispatch({ type: 'SET_DATE_RANGE', payload: range }),
        setEnforcement: (enforcement: EnforcementType) => dispatch({ type: 'SET_ENFORCEMENT', payload: enforcement }),
        setAutoRefresh: (enabled: boolean) => dispatch({ type: 'SET_AUTO_REFRESH', payload: enabled }),
        setFileDateRange: (range: DateRangeType) => dispatch({ type: 'SET_FILE_DATE_RANGE', payload: range }),
        toggleStatusFilter: (status: string) => dispatch({ type: 'TOGGLE_STATUS_FILTER', payload: status }),
        toggleQuarantineFilter: (filter: string) => dispatch({ type: 'TOGGLE_QUARANTINE_FILTER', payload: filter }),
        toggleThreatScannerFilter: (scanner: string) => dispatch({ type: 'TOGGLE_THREAT_SCANNER_FILTER', payload: scanner }),
        toggleValidationTypeFilter: (type: string) => dispatch({ type: 'TOGGLE_VALIDATION_TYPE_FILTER', payload: type }),

        // Menu anchors
        openFilterMenu: (event: React.MouseEvent<HTMLElement>) =>
            dispatch({ type: 'SET_FILTER_MENU_ANCHOR', payload: event.currentTarget }),
        closeFilterMenu: () => dispatch({ type: 'SET_FILTER_MENU_ANCHOR', payload: null }),
        openQuarantineFilterMenu: (event: React.MouseEvent<HTMLElement>) =>
            dispatch({ type: 'SET_QUARANTINE_FILTER_MENU_ANCHOR', payload: event.currentTarget }),
        closeQuarantineFilterMenu: () => dispatch({ type: 'SET_QUARANTINE_FILTER_MENU_ANCHOR', payload: null }),
        openAutoRejectedFilterMenu: (event: React.MouseEvent<HTMLElement>) =>
            dispatch({ type: 'SET_AUTO_REJECTED_FILTER_MENU_ANCHOR', payload: event.currentTarget }),
        closeAutoRejectedFilterMenu: () => dispatch({ type: 'SET_AUTO_REJECTED_FILTER_MENU_ANCHOR', payload: null }),

        // Selection
        toggleQuarantinedCheck: (id: string, checked: boolean) =>
            dispatch({ type: 'TOGGLE_QUARANTINED_CHECKED', payload: { id, checked } }),
        selectAllQuarantined: (scans: ScanResult[]) =>
            dispatch({ type: 'SELECT_ALL_QUARANTINED', payload: scans }),
        deselectAllQuarantined: () => dispatch({ type: 'DESELECT_ALL_QUARANTINED' }),
        setFilesChecked: (fileIds: Set<string>) =>
            dispatch({ type: 'SET_FILES_CHECKED', payload: fileIds }),

        // Dialogs
        openAllowDialog: () => dispatch({ type: 'OPEN_CONFIRM_DIALOG', payload: 'allow' }),
        openBlockDialog: () => dispatch({ type: 'OPEN_CONFIRM_DIALOG', payload: 'block' }),
        closeConfirmDialog: () => dispatch({ type: 'CLOSE_CONFIRM_DIALOG' }),
        executeConfirmAction,
        openFileDialog: (action: FileActionType) => dispatch({ type: 'OPEN_FILE_DIALOG', payload: action }),
        closeFileDialog: () => dispatch({ type: 'CLOSE_FILE_DIALOG' }),
        executeFileAction,
    }), [dispatch, executeConfirmAction, executeFileAction]);
};
