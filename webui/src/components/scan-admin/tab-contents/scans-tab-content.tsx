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
import { ScanCard } from '../scan-card';
import { SearchToolbar, CountsToolbar } from '../toolbars';
import { AutoRefresh } from '../common';
import { useScansTab } from '../../../hooks/scan-admin';
import { useTheme } from '@mui/material/styles';

/**
 * Scans tab component that displays an overview of all extension scans.
 * Uses the useScansTab hook to consume context.
 */
export const ScansTabContent: FunctionComponent = () => {
    const theme = useTheme();
    const {
        scans,
        isLoading,
        lastRefreshed,
        autoRefresh,
        onAutoRefreshChange,
        counts,
        search,
        globalFilters,
        statusFilters,
        pagination,
    } = useScansTab();

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
                    filters={[
                        { label: 'Running', value: 'running', checked: statusFilters.filters.has('running'), onChange: statusFilters.toggle },
                        { label: 'Passed', value: 'PASSED', checked: statusFilters.filters.has('PASSED'), onChange: statusFilters.toggle },
                        { label: 'Quarantined', value: 'QUARANTINED', checked: statusFilters.filters.has('QUARANTINED'), onChange: statusFilters.toggle },
                        { label: 'Auto Rejected', value: 'AUTO REJECTED', checked: statusFilters.filters.has('AUTO REJECTED'), onChange: statusFilters.toggle },
                        { label: 'Error', value: 'ERROR', checked: statusFilters.filters.has('ERROR'), onChange: statusFilters.toggle },
                    ]}
                />
                <CountsToolbar
                    counts={[
                        { label: 'Total', value: counts.total, color: 'text.primary' },
                        { label: 'Running', value: counts.started + counts.validating + counts.scanning, color: 'secondary.main' },
                        { label: 'Passed', value: counts.passed, color: theme.palette.passed.dark },
                        { label: 'Quarantined', value: counts.quarantined, color: theme.palette.quarantined.dark },
                        { label: 'Auto Rejected', value: counts.autoRejected, color: theme.palette.rejected.dark },
                        { label: 'Error', value: counts.error, color: theme.palette.errorStatus.dark },
                    ]}
                    dateRange={globalFilters.dateRange}
                    onDateRangeChange={globalFilters.setDateRange}
                    enforcement={globalFilters.enforcement}
                    onEnforcementChange={globalFilters.setEnforcement}
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
            ) : scans.length === 0 ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
                    <Typography variant='h6' color='text.secondary'>
                        No scans found
                    </Typography>
                    <Typography variant='body2' color='text.secondary' sx={{ mt: 1 }}>
                        {search.hasActiveSearch ? 'Try adjusting your search query' : 'Running and completed scans will appear here'}
                    </Typography>
                </Box>
            ) : (
                scans.map((scan) => (
                    <ScanCard key={scan.id} scan={scan} />
                ))
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
