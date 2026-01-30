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
import { useAutoRejectedTab } from '../../../hooks/scan-admin';

/**
 * Auto Rejected tab component that displays extensions that failed validation.
 * Uses the useAutoRejectedTab hook to consume context.
 */
export const AutoRejectedTabContent: FunctionComponent = () => {
    const {
        scans,
        isLoading,
        lastRefreshed,
        autoRefresh,
        onAutoRefreshChange,
        totalCount,
        search,
        globalFilters,
        validationTypeFilters,
        pagination,
        hasValidationTypes,
    } = useAutoRejectedTab();

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
                />
                <CountsToolbar
                    counts={[
                        { label: 'Total', value: totalCount, color: 'text.primary' },
                    ]}
                    filterOptions={validationTypeFilters.availableValidationTypes.map(type => ({
                        label: type,
                        value: type,
                        checked: validationTypeFilters.filters.has(type),
                    }))}
                    onFilterOptionToggle={validationTypeFilters.toggle}
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
            ) : (!hasValidationTypes || scans.length === 0) ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
                    <Typography variant='h6' color='text.secondary'>
                        No auto rejected extensions
                    </Typography>
                    <Typography variant='body2' color='text.secondary' sx={{ mt: 1 }}>
                        Extensions that fail validation will appear here
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
