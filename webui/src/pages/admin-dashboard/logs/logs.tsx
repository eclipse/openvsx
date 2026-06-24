/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FC, useState, useEffect, useMemo } from 'react';
import {
    Box,
    Paper,
    Typography,
    Alert,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    SelectChangeEvent
} from '@mui/material';
import { DataGrid, GridColDef, GridPaginationModel, GridRenderCellParams } from '@mui/x-data-grid';
import type { Log } from '../../../extension-registry-types';
import { handleError } from '../../../utils';
import { createMultiSelectFilterOperators } from '../components';
import { useLogs } from './use-logs';

type PeriodFilter = '' | 'P1D' | 'P7D' | 'P30D' | 'P90D' | 'P1Y';

const periodOptions: { value: PeriodFilter; label: string }[] = [
    { value: '', label: 'All Time' },
    { value: 'P1D', label: 'Last 24 Hours' },
    { value: 'P7D', label: 'Last 7 Days' },
    { value: 'P30D', label: 'Last 30 Days' },
    { value: 'P90D', label: 'Last 90 Days' },
    { value: 'P1Y', label: 'Last Year' }
];

export const Logs: FC = () => {
    const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
        page: 0,
        pageSize: 20
    });
    const [periodFilter, setPeriodFilter] = useState<PeriodFilter>('');
    const [errorDismissed, setErrorDismissed] = useState(false);

    const {
        data,
        isFetching: loading,
        error: loadError
    } = useLogs(paginationModel.page, paginationModel.pageSize, periodFilter || undefined);

    const logs: Log[] = useMemo(() => (data?.content ? [...data.content] : []), [data]);
    const totalElements = data?.page.totalElements ?? 0;
    const logsWithId = useMemo(() => logs.map((log, index) => ({ ...log, id: index })), [logs]);

    // A fresh load error should be shown again even if a previous one was dismissed.
    useEffect(() => {
        setErrorDismissed(false);
    }, [loadError]);

    const error = loadError && !errorDismissed ? handleError(loadError as Error) : null;

    const handlePaginationModelChange = (newModel: GridPaginationModel) => {
        setPaginationModel(newModel);
    };

    const handlePeriodChange = (event: SelectChangeEvent) => {
        setPeriodFilter(event.target.value as PeriodFilter);
        // Reset to first page when filter changes
        setPaginationModel(prev => ({ ...prev, page: 0 }));
    };

    // Extract unique values for filter dropdowns
    const userOptions = useMemo(() => [...new Set(logs.map(l => l.user).filter(Boolean))], [logs]);

    const columns: GridColDef[] = [
        {
            field: 'timestamp',
            headerName: 'Timestamp',
            width: 200,
            sortable: false,
            renderCell: (params: GridRenderCellParams<Log>) => {
                if (!params.value) return null;
                const date = new Date(params.value as string);
                return <Typography variant='caption'>{date.toLocaleString()}</Typography>;
            }
        },
        {
            field: 'user',
            headerName: 'User',
            flex: 1,
            minWidth: 150,
            sortable: false,
            filterOperators: createMultiSelectFilterOperators(userOptions)
        },
        {
            field: 'message',
            headerName: 'Message',
            flex: 5,
            minWidth: 300,
            sortable: false
        }
    ];

    return (
        <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mb: 3,
                    flexWrap: 'wrap',
                    gap: 2,
                    width: '100%'
                }}>
                <Typography variant='h4' component='h1'>
                    Admin Logs
                </Typography>
                <FormControl sx={{ minWidth: 200 }} size='small'>
                    <InputLabel id='period-filter-label'>Time Period</InputLabel>
                    <Select
                        labelId='period-filter-label'
                        id='period-filter'
                        value={periodFilter}
                        label='Time Period'
                        onChange={handlePeriodChange}>
                        {periodOptions.map(option => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </Box>

            {error && (
                <Alert severity='error' sx={{ mb: 2 }} onClose={() => setErrorDismissed(true)}>
                    {error}
                </Alert>
            )}

            {!loading && !error && logs.length === 0 && (
                <Paper elevation={0} sx={{ p: 3, textAlign: 'center' }}>
                    <Typography color='textSecondary' gutterBottom>
                        No logs found for the selected time period.
                    </Typography>
                </Paper>
            )}

            {!error && logs.length > 0 && (
                <Paper
                    elevation={0}
                    sx={{ flex: 1, minHeight: 400, width: '100%', display: 'flex', flexDirection: 'column' }}>
                    <DataGrid
                        rows={logsWithId}
                        columns={columns}
                        paginationMode='server'
                        rowCount={totalElements}
                        paginationModel={paginationModel}
                        onPaginationModelChange={handlePaginationModelChange}
                        pageSizeOptions={[20, 35, 50, 100]}
                        loading={loading}
                        disableRowSelectionOnClick
                        sx={{
                            flex: 1
                        }}
                    />
                </Paper>
            )}
        </Box>
    );
};
