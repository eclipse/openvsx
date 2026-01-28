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

import React, { FunctionComponent } from 'react';
import { Box, Typography, Pagination, CircularProgress } from '@mui/material';
import { SearchToolbar, CountsToolbar } from '../toolbars';
import { FileTable, AutoRefresh } from '../common';
import { useAllowListTab } from '../../../hooks/scan-admin';
import { useScanContext } from '../../../context/scan-admin';
import { useTheme } from '@mui/material/styles';

/**
 * Allowed Files tab component that displays files that have been allowed.
 * Uses the useAllowListTab hook to consume context.
 */
export const AllowListTabContent: FunctionComponent = () => {
    const theme = useTheme();
    const { state, actions } = useScanContext();
    const {
        files,
        isLoading,
        fileCount,
        search,
        selection,
        setFilesChecked,
        fileActions,
        pagination,
        lastRefreshed,
        autoRefresh,
        onAutoRefreshChange,
    } = useAllowListTab();

    return (
        <>
            <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                <SearchToolbar
                    publisherQuery={search.publisherQuery}
                    namespaceQuery={search.namespaceQuery}
                    nameQuery={search.nameQuery}
                    onPublisherChange={search.handlePublisherChange}
                    onNamespaceChange={search.handleNamespaceChange}
                    onNameChange={search.handleNameChange}
                    actionButtons={[
                        {
                            label: 'BLOCK',
                            color: theme.palette.blocked!,
                            disabled: !fileActions.canPerformAction,
                            onClick: fileActions.openBlockDialog,
                        },
                        {
                            label: 'DELETE',
                            color: theme.palette.secondary.main,
                            disabled: !fileActions.canPerformAction,
                            onClick: fileActions.openDeleteDialog,
                        },
                    ]}
                    selectedCount={selection.selectedCount}
                />
                <CountsToolbar
                    counts={[
                        { label: 'Allowed Files', value: fileCount, color: theme.palette.allowed },
                    ]}
                    dateRange={state.fileDateRange}
                    onDateRangeChange={actions.setFileDateRange}
                />
                <AutoRefresh
                    lastRefreshed={lastRefreshed}
                    autoRefresh={autoRefresh}
                    onAutoRefreshChange={onAutoRefreshChange}
                />
            </Box>
            {isLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                    <CircularProgress color='secondary' />
                </Box>
            ) : files.length === 0 ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
                    <Typography variant='h6' color='text.secondary'>
                        No allowed files
                    </Typography>
                    <Typography variant='body2' color='text.secondary' sx={{ mt: 1 }}>
                        Files that have been allowed will appear here
                    </Typography>
                </Box>
            ) : (
                <FileTable
                    files={files}
                    type='allowed'
                    selectedFiles={selection.checked}
                    onSelectionChange={setFilesChecked}
                />
            )}
            {pagination.totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3, mb: 2 }}>
                    <Pagination
                        count={pagination.totalPages}
                        page={pagination.currentPage + 1}
                        onChange={(_, page) => pagination.goToPage(page - 1)}
                        disabled={isLoading}
                        color='secondary'
                        size='large'
                        showFirstButton
                        showLastButton
                    />
                </Box>
            )}
        </>
    );
};
