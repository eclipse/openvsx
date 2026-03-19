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

import { FC, useContext, useState, useEffect, useRef, useCallback } from "react";
import {
    Box,
    Typography,
    Paper,
    type PaperProps,
    Chip,
    Stack,
    Alert,
    CircularProgress,
    Button,
    Divider
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import EditIcon from "@mui/icons-material/Edit";
import { useParams, useNavigate } from "react-router-dom";
import { MainContext } from "../../../context";
import type { Customer } from "../../../extension-registry-types";
import { handleError } from "../../../utils";
import { AdminDashboardRoutes } from "../admin-dashboard";
import { UsageStatsChart } from "../usage-stats/usage-stats-chart";
import { useUsageStats } from "../usage-stats/use-usage-stats";
import { CustomerFormDialog } from "./customer-form-dialog";

const sectionPaperProps: PaperProps = { elevation: 1, sx: { p: 3, mb: 3 } };

export const CustomerDetails: FC = () => {
    const { customer: customerName } = useParams<{ customer: string }>();
    const navigate = useNavigate();
    const abortController = useRef(new AbortController());
    const { service } = useContext(MainContext);

    const [customer, setCustomer] = useState<Customer | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [formDialogOpen, setFormDialogOpen] = useState(false);

    const { usageStats, error: statsError, startDate, setStartDate } = useUsageStats(customerName);

    const loadCustomer = useCallback(async () => {
        if (!customerName) return;
        try {
            setLoading(true);
            setError(null);
            const data = await service.admin.getCustomers(abortController.current);
            const found = data.customers.find(c => c.name === customerName);
            if (found) {
                setCustomer(found);
            } else {
                setError(`Customer "${customerName}" not found.`);
            }
        } catch (err) {
            setError(handleError(err as Error));
        } finally {
            setLoading(false);
        }
    }, [service, customerName]);

    useEffect(() => {
        loadCustomer();
        return () => abortController.current.abort();
    }, [loadCustomer]);

    const handleFormSubmit = async (updatedCustomer: Customer) => {
        if (customer) {
            await service.admin.updateCustomer(abortController.current, customer.name, updatedCustomer);
            await loadCustomer();
        }
    };

    if (loading) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (error || statsError) {
        return (
            <Box sx={{ p: 3 }}>
                <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(AdminDashboardRoutes.CUSTOMERS)} sx={{ mb: 2 }}>
                    Back to Customers
                </Button>
                <Alert severity='error'>{error || statsError}</Alert>
            </Box>
        );
    }

    if (!customer) {
        return null;
    }

    const tier = customer.tier;

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(AdminDashboardRoutes.CUSTOMERS)} sx={{ mr: 2 }}>
                    Back to Customers
                </Button>
                <Typography variant='h4' component='h1'>
                    {customer.name}
                </Typography>
                <Button
                    variant='outlined'
                    startIcon={<EditIcon />}
                    onClick={() => setFormDialogOpen(true)}
                    sx={{ ml: 'auto' }}
                >
                    Edit
                </Button>
            </Box>

            {/* General Information */}
            <Paper {...sectionPaperProps}>
                <Typography variant='h6' gutterBottom>
                    General Information
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Stack spacing={2}>
                    <Box>
                        <Typography variant='subtitle2' color='text.secondary'>Name</Typography>
                        <Typography variant='body1'>{customer.name}</Typography>
                    </Box>
                    <Box>
                        <Typography variant='subtitle2' color='text.secondary'>State</Typography>
                        <Chip
                            label={customer.state}
                            size='small'
                            color={customer.state === 'ENFORCEMENT' ? 'error' : 'warning'}
                        />
                    </Box>
                </Stack>
            </Paper>

            {/* Tier */}
            <Paper {...sectionPaperProps}>
                <Typography variant='h6' gutterBottom>
                    Tier
                </Typography>
                <Divider sx={{ mb: 2 }} />
                {tier ? (
                    <Stack spacing={1.5}>
                        <Box>
                            <Typography variant='subtitle2' color='text.secondary'>Name</Typography>
                            <Chip label={tier.name} size='small' />
                        </Box>
                        <Box>
                            <Typography variant='subtitle2' color='text.secondary'>Type</Typography>
                            <Typography variant='body2'>{tier.tierType}</Typography>
                        </Box>
                        <Box>
                            <Typography variant='subtitle2' color='text.secondary'>Capacity</Typography>
                            <Typography variant='body2'>{tier.capacity} requests / {tier.duration}s</Typography>
                        </Box>
                        <Box>
                            <Typography variant='subtitle2' color='text.secondary'>Refill Strategy</Typography>
                            <Typography variant='body2'>{tier.refillStrategy}</Typography>
                        </Box>
                        {tier.description && (
                            <Box>
                                <Typography variant='subtitle2' color='text.secondary'>Description</Typography>
                                <Typography variant='body2'>{tier.description}</Typography>
                            </Box>
                        )}
                    </Stack>
                ) : (
                    <Typography variant='body2' color='text.secondary'>No tier assigned</Typography>
                )}
            </Paper>

            {/* CIDR Blocks */}
            <Paper {...sectionPaperProps}>
                <Typography variant='h6' gutterBottom>
                    CIDR Blocks
                </Typography>
                <Divider sx={{ mb: 2 }} />
                {customer.cidrBlocks.length > 0 ? (
                    <Stack direction='row' spacing={0.5} flexWrap='wrap' useFlexGap>
                        {customer.cidrBlocks.map((cidr) => (
                            <Chip key={cidr} label={cidr} size='small' variant='outlined' />
                        ))}
                    </Stack>
                ) : (
                    <Typography variant='body2' color='text.secondary'>None configured</Typography>
                )}
            </Paper>

            {/* Usage Statistics */}
            <Paper {...sectionPaperProps}>
                <Typography variant='h6' gutterBottom>
                    Usage Statistics
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <UsageStatsChart
                    usageStats={usageStats}
                    customer={customer}
                    startDate={startDate}
                    onStartDateChange={setStartDate}
                    embedded
                />
            </Paper>

            <CustomerFormDialog
                open={formDialogOpen}
                customer={customer}
                onClose={() => setFormDialogOpen(false)}
                onSubmit={handleFormSubmit}
            />
        </Box>
    );
};
