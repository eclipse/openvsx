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

import React from 'react';
import {
    ScanState,
    ScanAction,
    DateRangeType,
    EnforcementType,
    FileActionType,
    FileDecision,
    ScanResult,
} from './scan-types';

// ============================================================================
// Context Types
// ============================================================================

export interface ScanContextValue {
    state: ScanState;
    dispatch: React.Dispatch<ScanAction>;
    actions: ScanActions;
    derived: DerivedData;
}

export interface ScanActions {
    // Tab
    setTab: (tab: number) => void;

    // Search
    setPublisherQuery: (query: string) => void;
    setNamespaceQuery: (query: string) => void;
    setNameQuery: (query: string) => void;
    handlePublisherChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    handleNamespaceChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    handleNameChange: (event: React.ChangeEvent<HTMLInputElement>) => void;

    // Pagination
    setPage: (page: number) => void;

    // Filters
    setDateRange: (range: DateRangeType) => void;
    setEnforcement: (enforcement: EnforcementType) => void;
    setAutoRefresh: (enabled: boolean) => void;
    setFileDateRange: (range: DateRangeType) => void;
    toggleStatusFilter: (status: string) => void;
    toggleQuarantineFilter: (filter: string) => void;
    toggleThreatScannerFilter: (scanner: string) => void;
    toggleValidationTypeFilter: (type: string) => void;

    // Menu anchors
    openFilterMenu: (event: React.MouseEvent<HTMLElement>) => void;
    closeFilterMenu: () => void;
    openQuarantineFilterMenu: (event: React.MouseEvent<HTMLElement>) => void;
    closeQuarantineFilterMenu: () => void;
    openAutoRejectedFilterMenu: (event: React.MouseEvent<HTMLElement>) => void;
    closeAutoRejectedFilterMenu: () => void;

    // Selection
    toggleQuarantinedCheck: (id: string, checked: boolean) => void;
    selectAllQuarantined: (scans: ScanResult[]) => void;
    deselectAllQuarantined: () => void;
    setFilesChecked: (fileIds: Set<string>) => void;

    // Dialogs
    openAllowDialog: () => void;
    openBlockDialog: () => void;
    closeConfirmDialog: () => void;
    executeConfirmAction: () => void;
    openFileDialog: (action: FileActionType) => void;
    closeFileDialog: () => void;
    executeFileAction: () => void;
}

export interface DerivedData {
    /** Selected quarantined extensions (for bulk allow/block actions) */
    selectedExtensions: ScanResult[];
    /** Selected files from Allowed/Blocked Files tabs */
    selectedFiles: FileDecision[];
    /** Total pages for current data set (scans or files depending on tab) */
    totalPages: number;
}

// ============================================================================
// Provider Props
// ============================================================================

export interface ScanProviderProps {
    children: React.ReactNode;
    service: any;
    handleError: (error: any) => void;
}
