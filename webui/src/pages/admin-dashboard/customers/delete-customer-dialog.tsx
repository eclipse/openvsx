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

import React, { FC, useState } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    Alert,
    CircularProgress,
    Box
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import type { Customer } from "../../../extension-registry-types";
import {handleError} from "../../../utils";

interface DeleteCustomerDialogProps {
    open: boolean;
    customer?: Customer;
    onClose: () => void;
    onConfirm: () => Promise<void>;
}

export const DeleteCustomerDialog: FC<DeleteCustomerDialogProps> = ({ open, customer, onClose, onConfirm }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleConfirm = async () => {
        try {
            setError(null);
            setLoading(true);
            await onConfirm();
            onClose();
        } catch (err: any) {
            setError(handleError(err));
            setLoading(false);
        }
    };

    if (!customer) return null;

    return (
        <Dialog open={open} onClose={onClose} maxWidth='sm' fullWidth>
            <DialogTitle>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <WarningIcon sx={{ color: 'warning.main' }} />
                    Delete Customer
                </Box>
            </DialogTitle>
            <DialogContent>
                {error && <Alert severity='error' sx={{ mb: 2 }}>{error}</Alert>}
                <Typography gutterBottom>
                    Are you sure you want to delete the customer <strong>{customer.name}</strong>?
                </Typography>
                {customer.tier?.name && (
                    <Typography variant='body2' color='textSecondary' sx={{ mt: 1 }}>
                        This customer is currently using the <strong>{customer.tier?.name}</strong> tier.
                    </Typography>
                )}
                <Typography variant='body2' color='error' sx={{ mt: 2 }}>
                    This action cannot be undone. All associated usage statistics will also be deleted.
                </Typography>
            </DialogContent>
            <DialogActions sx={{ p: 2 }}>
                <Button onClick={onClose} disabled={loading}>
                    Cancel
                </Button>
                <Button
                    onClick={handleConfirm}
                    variant='contained'
                    color='error'
                    disabled={loading}
                    startIcon={loading ? <CircularProgress size={20} /> : undefined}
                >
                    Delete
                </Button>
            </DialogActions>
        </Dialog>
    );
};
