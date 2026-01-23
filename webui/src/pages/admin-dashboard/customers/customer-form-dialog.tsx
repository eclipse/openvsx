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

import React, { FC, useState, useEffect, useRef } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Button,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    CircularProgress,
    Alert,
    Box
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { type Customer, EnforcementState, type Tier } from "../../../extension-registry-types";
import { MainContext } from "../../../context";

interface CustomerFormDialogProps {
    open: boolean;
    customer?: Customer;
    onClose: () => void;
    onSubmit: (formData: Customer) => Promise<void>;
}

export const CustomerFormDialog: FC<CustomerFormDialogProps> = ({ open, customer, onClose, onSubmit }) => {
    const abortController = useRef<AbortController>(new AbortController());
    const { service } = React.useContext(MainContext);
    const [formData, setFormData] = useState<Customer>({
        name: '',
        tier: undefined,
        state: EnforcementState.ENFORCEMENT,
        cidrBlocks: []
    });
    const [tiers, setTiers] = useState<Tier[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadTiers = async () => {
        try {
            const data = await service.admin.getTiers(abortController.current!);
            setTiers(data.tiers);
        } catch (err: any) {
            console.error('Failed to load tiers:', err);
        }
    };

    useEffect(() => {
        loadTiers();
        return () => abortController.current.abort();
    }, []);

    useEffect(() => {
        if (customer) {
            setFormData({
                name: customer.name,
                tier: customer.tier,
                state: customer.state,
                cidrBlocks: customer.cidrBlocks
            });
        } else {
            setFormData({
                name: '',
                tier: undefined,
                state: EnforcementState.ENFORCEMENT,
                cidrBlocks: []
            });
        }
        setError(null);
    }, [open, customer]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | SelectChangeEvent) => {
        const { name, value } = e.target;

        if (name === 'tierName') {
            const tier = tiers.find((tier) => tier.name === value);
            setFormData(prev => ({
                ...prev,
                tier: tier,
            }));
        } else if (name === 'cidrBlocks') {
            const cidrsBlocks = value.split(",")
            setFormData(prev => ({
                ...prev,
                cidrBlocks: cidrsBlocks,
            }));
        } else {
            setFormData(prev => ({ ...prev, [name]: value }));
        }
    };

    const handleSubmit = async () => {
        setError(null);
        setLoading(true);

        try {
            // validate required fields
            if (!formData.name.trim()) {
                throw new Error('Customer name is required');
            }

            if (!formData.state) {
                throw new Error('State is required');
            }

            await onSubmit(formData);
            onClose();
        } catch (err: any) {
            setError(err.message || 'An error occurred while saving the customer');
        } finally {
            setLoading(false);
        }
    };

    const isEditMode = !!customer;
    const title = isEditMode ? 'Edit Customer' : 'Create New Customer';

    return (
        <Dialog open={open} onClose={onClose} maxWidth='sm' fullWidth>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
                {error && <Alert severity='error'>{error}</Alert>}

                <TextField
                    label='Customer Name'
                    name='name'
                    value={formData.name}
                    onChange={handleChange}
                    fullWidth
                    placeholder='e.g., Acme Corp, TechStart Inc'
                    required={true}
                    disabled={loading}
                />

                <FormControl fullWidth disabled={loading}>
                    <InputLabel>Tier</InputLabel>
                    <Select
                        name='tierName'
                        value={formData.tier?.name ?? ''}
                        onChange={handleChange}
                        label='Tier'
                    >
                        {tiers.map(tier => (
                            <MenuItem key={tier.name} value={tier.name}>
                                {tier.name}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <FormControl fullWidth disabled={loading} required={true}>
                    <InputLabel>State</InputLabel>
                    <Select
                        name='state'
                        value={formData.state}
                        onChange={handleChange}
                        label='State'
                    >
                        {Object.keys(EnforcementState).map(key => (
                            <MenuItem key={key} value={EnforcementState[key as keyof typeof EnforcementState]}>
                                {key}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <TextField
                    label='CIDR Blocks'
                    name='cidrBlocks'
                    value={formData.cidrBlocks?.join(",") ?? ''}
                    onChange={handleChange}
                    fullWidth
                    multiline
                    rows={3}
                    placeholder='e.g., 192.168.1.0/24,192.168.2.0/24'
                    helperText='Comma-separated list of CIDR blocks (optional)'
                    disabled={loading}
                />

              </Box>
            </DialogContent>

            <DialogActions sx={{ p: 2 }}>
                <Button onClick={onClose} disabled={loading}>
                    Cancel
                </Button>
                <Button
                    onClick={handleSubmit}
                    variant='contained'
                    disabled={loading}
                    startIcon={loading ? <CircularProgress size={20} /> : undefined}
                >
                    {isEditMode ? 'Update' : 'Create'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
