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

import { useCallback } from 'react';
import { ScanState, ScanAction } from './scan-types';

// ============================================================================
// Confirm Action Hook
// ============================================================================

/**
 * Hook that returns the async confirm action handler for allow/block operations
 * on selected quarantined scans. Makes a single API call to persist the decision.
 * The backend handles adding enforced threat files to the allow/block list.
 */
export const useConfirmAction = (
    service: any,
    state: ScanState,
    dispatch: React.Dispatch<ScanAction>,
    handleErrorRef: React.MutableRefObject<(error: any) => void>
) => {
    return useCallback(async () => {
        const abortController = new AbortController();

        try {
            // Get selected scan IDs
            const selectedScanIds: string[] = [];
            for (const id in state.quarantinedChecked) {
                if (state.quarantinedChecked[id]) {
                    selectedScanIds.push(id);
                }
            }

            if (selectedScanIds.length === 0) {
                dispatch({ type: 'CLOSE_CONFIRM_DIALOG' });
                return;
            }

            const decision = state.confirmAction === 'allow' ? 'allowed' : 'blocked';

            // Backend handles adding enforced threat files to allow/block list automatically
            const scanResponse = await service.admin.makeScanDecision(abortController, {
                scanIds: selectedScanIds,
                decision,
            });

            if (scanResponse.error) {
                handleErrorRef.current(new Error(scanResponse.error));
                return;
            }

            // Update local state and trigger refresh
            dispatch({ type: 'EXECUTE_CONFIRM_ACTION' });
            dispatch({ type: 'TRIGGER_REFRESH' });

        } catch (err: any) {
            if (!abortController.signal.aborted) {
                handleErrorRef.current(err);
            }
        }
    }, [service, state.quarantinedChecked, state.confirmAction, dispatch, handleErrorRef]);
};

// ============================================================================
// File Action Hook
// ============================================================================

/**
 * Hook that returns the async file action handler for allow/block/delete operations
 * on selected files in Allowed/Blocked Files tabs.
 */
export const useFileAction = (
    service: any,
    state: ScanState,
    dispatch: React.Dispatch<ScanAction>,
    handleErrorRef: React.MutableRefObject<(error: any) => void>
) => {
    return useCallback(async () => {
        const abortController = new AbortController();

        try {
            if (state.filesChecked.size === 0) {
                dispatch({ type: 'CLOSE_FILE_DIALOG' });
                return;
            }

            const selectedFileIds = Array.from(state.filesChecked).map(id => parseInt(id, 10));

            if (state.fileActionType === 'delete') {
                const response = await service.admin.deleteFileDecisions(abortController, {
                    fileIds: selectedFileIds,
                });

                if (response.error) {
                    handleErrorRef.current(new Error(response.error));
                    return;
                }
            } else {
                const decision = state.fileActionType === 'allow' ? 'allowed' : 'blocked';

                const selectedFiles = state.files.filter(file => state.filesChecked.has(file.id));
                const fileHashes = selectedFiles.map(file => file.fileHash);

                const response = await service.admin.makeFileDecision(abortController, {
                    fileHashes,
                    decision,
                });

                if (response.error) {
                    handleErrorRef.current(new Error(response.error));
                    return;
                }
            }

            // Update local state and trigger refresh
            dispatch({ type: 'SET_FILES_CHECKED', payload: new Set() });
            dispatch({ type: 'CLOSE_FILE_DIALOG' });
            dispatch({ type: 'RESET_PAGE' }); // Go back to first page after action
            dispatch({ type: 'TRIGGER_REFRESH' });

        } catch (err: any) {
            if (!abortController.signal.aborted) {
                handleErrorRef.current(err);
            }
        }
    }, [service, state.filesChecked, state.fileActionType, state.files, dispatch, handleErrorRef]);
};
