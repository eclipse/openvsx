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

import React, { FC, useState, useEffect } from 'react';
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
import { RefillStrategy, type Tier } from "../../../extension-registry-types";

type DurationUnit = 'seconds' | 'minutes' | 'hours';

const DURATION_MULTIPLIERS: Record<DurationUnit, number> = {
    seconds: 1,
    minutes: 60,
    hours: 3600
};

function formatDuration(duration: number): [number, DurationUnit] {
    const hours = Math.floor(duration / 3600);
    if (hours > 0) {
        return [hours, "hours"];
    }

    const minutes = Math.floor(duration / 60);
    if (minutes > 0) {
        return [minutes, "minutes"];
    }

    return [duration, "seconds"];
}

interface TierFormDialogProps {
    open: boolean;
    tier?: Tier;
    onClose: () => void;
    onSubmit: (formData: Tier) => Promise<void>;
}

export const TierFormDialog: FC<TierFormDialogProps> = ({ open, tier, onClose, onSubmit }) => {
    const [formData, setFormData] = useState<Tier>({
        name: '',
        description: '',
        capacity: 100,
        duration: 3600,
        refillStrategy: RefillStrategy.INTERVAL
    } as Tier);
    const [durationValue, setDurationValue] = useState(1);
    const [durationUnit, setDurationUnit] = useState<DurationUnit>('hours');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const getDurationInSeconds = (): number => {
        return durationValue * DURATION_MULTIPLIERS[durationUnit];
    };

    useEffect(() => {
        if (tier) {
            setFormData(_ => ({
                name: tier.name,
                description: tier.description || '',
                capacity: tier.capacity,
                duration: tier.duration,
                refillStrategy: tier.refillStrategy as any
            } as Tier));
            // Convert duration seconds to value/unit for display
            const [value, unit] = formatDuration(tier.duration);
            setDurationValue(value);
            setDurationUnit(unit);
        } else {
            setFormData(prev => ({
                ...prev,
                name: '',
                description: '',
                capacity: 100,
                duration: 3600,
                refillStrategy: RefillStrategy.INTERVAL
            }));
            setDurationValue(1);
            setDurationUnit('hours');
        }
        setError(null);
    }, [open, tier]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }> | SelectChangeEvent) => {
        const { name, value } = e.target as any;
        setFormData((prev: Tier) => ({
            ...prev,
            [name]: name === 'capacity' || name === 'duration' ? Number.parseInt(value as string, 10) : value
        } as Tier));
    };

    const handleSubmit = async () => {
        setError(null);
        setLoading(true);

        try {
            // Validate required fields
            if (!formData.name.trim()) {
                throw new Error('Tier name is required');
            }

            if (formData.capacity <= 0) {
                throw new Error('Capacity must be greater than 0');
            }

            if (durationValue <= 0) {
                throw new Error('Duration must be greater than 0');
            }

            const durationInSeconds = getDurationInSeconds();
            await onSubmit({
                ...formData,
                duration: durationInSeconds
            });
            onClose();
        } catch (err: any) {
            setError(err.message || 'An error occurred while saving the tier');
        } finally {
            setLoading(false);
        }
    };

    const isEditMode = !!tier;
    const title = isEditMode ? 'Edit Tier' : 'Create New Tier';

    return (
        <Dialog open={open} onClose={onClose} maxWidth='sm' fullWidth>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
                {error && <Alert severity='error'>{error}</Alert>}

                <TextField
                    label='Tier Name'
                    name='name'
                    value={formData.name}
                    onChange={handleChange}
                    fullWidth
                    placeholder='e.g., Professional, Enterprise'
                    required={true}
                    disabled={loading}
                />

                <TextField
                    label='Description'
                    name='description'
                    value={formData.description}
                    onChange={handleChange}
                    fullWidth
                    multiline
                    rows={3}
                    placeholder='Optional description for this tier'
                    disabled={loading}
                />

                <TextField
                    label='Capacity'
                    name='capacity'
                    type='number'
                    value={formData.capacity}
                    onChange={handleChange}
                    fullWidth
                    inputProps={{ min: '1' }}
                    disabled={loading}
                    required={true}
                    helperText='Maximum number of requests allowed per interval'
                />

                <Box sx={{ display: 'flex', gap: 2 }}>
                    <TextField
                        label='Duration'
                        type='number'
                        value={durationValue}
                        onChange={(e) => setDurationValue(Math.max(1, Number.parseInt(e.target.value, 10) || 0))}
                        inputProps={{ min: '1' }}
                        disabled={loading}
                        required={true}
                        sx={{ flex: 1 }}
                    />
                    <FormControl disabled={loading} required={true} sx={{ minWidth: 150 }}>
                        <InputLabel>Unit</InputLabel>
                        <Select
                            value={durationUnit}
                            onChange={(e: SelectChangeEvent) => setDurationUnit(e.target.value as DurationUnit)}
                            label='Unit'
                        >
                            <MenuItem value='seconds'>Seconds</MenuItem>
                            <MenuItem value='minutes'>Minutes</MenuItem>
                            <MenuItem value='hours'>Hours</MenuItem>
                        </Select>
                    </FormControl>
                </Box>
                <Box sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>
                    = {getDurationInSeconds().toLocaleString()} seconds
                </Box>

                <FormControl fullWidth disabled={loading} required={true}>
                    <InputLabel>Refill Strategy</InputLabel>
                    <Select
                        name='refillStrategy'
                        value={formData.refillStrategy || ''}
                        onChange={(e: SelectChangeEvent) => {
                            const { name, value } = e.target;
                            setFormData((prev: Tier) => ({
                                ...prev,
                                [name]: value
                            } as Tier));
                        }}
                        label='Refill Strategy'
                    >
                        {Object.keys(RefillStrategy).map(strategy => (
                            <MenuItem key={strategy} value={RefillStrategy[strategy as keyof typeof RefillStrategy]}>
                                {strategy}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>


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
