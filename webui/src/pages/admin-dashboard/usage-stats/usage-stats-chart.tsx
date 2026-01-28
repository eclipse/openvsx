/*
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
 */

import React, { FC, useMemo } from "react";
import {
    Box,
    Paper,
    Typography,
    Alert,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    SelectChangeEvent,
    useTheme,
    Stack
} from "@mui/material";
import { BarChart } from "@mui/x-charts/BarChart";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDateFns } from "@mui/x-date-pickers/AdapterDateFns";
import type { UsageStats, UsageStatsPeriod } from "../../../extension-registry-types";
import { aggregateByPeriod } from "./usage-stats-utils";

interface UsageStatsChartProps {
    usageStats: readonly UsageStats[];
    period: UsageStatsPeriod;
    startDate: Date | null;
    endDate: Date | null;
    onPeriodChange: (event: SelectChangeEvent<UsageStatsPeriod>) => void;
    onStartDateChange: (date: Date | null) => void;
    onEndDateChange: (date: Date | null) => void;
}

export const UsageStatsChart: FC<UsageStatsChartProps> = ({
    usageStats,
    period,
    startDate,
    endDate,
    onPeriodChange,
    onStartDateChange,
    onEndDateChange
}) => {
    const theme = useTheme();

    const aggregatedData = useMemo(
        () => aggregateByPeriod([...usageStats], period),
        [usageStats, period]
    );

    const totalRequests = useMemo(
        () => aggregatedData.reduce((sum, d) => sum + d.count, 0),
        [aggregatedData]
    );

    if (usageStats.length === 0) {
        return <Alert severity='info'>No usage data available for this customer.</Alert>;
    }

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <Paper sx={{ p: 2, mb: 3 }}>
                <Typography variant='subtitle2' gutterBottom color='text.secondary'>
                    Filters
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems='center'>
                    <DatePicker
                        label='Start Date'
                        value={startDate}
                        onChange={onStartDateChange}
                        slotProps={{ textField: { size: 'small' } }}
                        maxDate={endDate || undefined}
                    />
                    <DatePicker
                        label='End Date'
                        value={endDate}
                        onChange={onEndDateChange}
                        slotProps={{ textField: { size: 'small' } }}
                        minDate={startDate || undefined}
                    />
                    <FormControl size='small' sx={{ minWidth: 150 }}>
                        <InputLabel id='period-select-label'>Aggregation</InputLabel>
                        <Select
                            labelId='period-select-label'
                            id='period-select'
                            value={period}
                            label='Aggregation'
                            onChange={onPeriodChange}
                        >
                            <MenuItem value='daily'>Daily</MenuItem>
                            <MenuItem value='weekly'>Weekly</MenuItem>
                            <MenuItem value='monthly'>Monthly</MenuItem>
                        </Select>
                    </FormControl>
                </Stack>
            </Paper>

            <Paper sx={{ p: 2 }}>
                <BarChart
                    xAxis={[{
                        scaleType: 'band',
                        data: aggregatedData.map(d => d.period),
                    }]}
                    series={[{
                        data: aggregatedData.map(d => d.count),
                        label: 'Request Count',
                        color: theme.palette.primary.main
                    }]}
                    height={400}
                    slotProps={{
                        legend: { position: { vertical: 'top', horizontal: 'right' } }
                    }}
                />
            </Paper>

            <Box mt={2}>
                <Typography variant='body2' color='text.secondary'>
                    Total requests in selected range: {totalRequests.toLocaleString()}
                    {usageStats.length > 0 && <> ({usageStats.length} data points)</>}
                </Typography>
            </Box>
        </LocalizationProvider>
    );
};