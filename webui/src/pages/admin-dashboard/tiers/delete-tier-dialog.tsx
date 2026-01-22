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
    CircularProgress,
    Alert
} from '@mui/material';
import type { Tier } from '../../../extension-registry-types';

interface DeleteTierDialogProps {
    open: boolean;
    tier?: Tier;
    onClose: () => void;
    onConfirm: () => Promise<void>;
}

export const DeleteTierDialog: FC<DeleteTierDialogProps> = ({ open, tier, onClose, onConfirm }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleConfirm = async () => {
        try {
            setError(null);
            setLoading(true);
            await onConfirm();
            onClose();
        } catch (err: any) {
            setError(err.message || 'An error occurred while deleting the tier');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth='sm' fullWidth>
            <DialogTitle>Delete Tier</DialogTitle>
            <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
                {error && <Alert severity='error'>{error}</Alert>}

                <Typography>
                    Are you sure you want to delete the tier <strong>{tier?.name}</strong>?
                </Typography>

                <Typography variant='body2' color='warning.main'>
                    This action cannot be undone. If this tier is assigned to customers, they will be affected.
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
