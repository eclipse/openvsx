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
import { CustomerState, type Customer, type Tier } from "../../../extension-registry-types";
import { MainContext } from "../../../context";

interface CustomerFormDialogProps {
    open: boolean;
    customer?: Customer;
    onClose: () => void;
    onSubmit: (formData: Customer) => Promise<void>;
}

export const CustomerFormDialog: FC<CustomerFormDialogProps> = ({ open, customer, onClose, onSubmit }) => {
    const abortController = useRef<AbortController>();
    const { service } = React.useContext(MainContext);
    const [formData, setFormData] = useState<Customer>({
        name: '',
        tierId: undefined,
        state: CustomerState.ENFORCEMENT,
        cidrBlocks: undefined
    });
    const [tiers, setTiers] = useState<Tier[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        abortController.current = new AbortController();
        
        const loadTiers = async () => {
            try {
                const data = await service.admin.getTiers(abortController.current!);
                setTiers(data.tiers);
            } catch (err: any) {
                console.error('Failed to load tiers:', err);
            }
        };
        
        loadTiers();

        return () => abortController.current?.abort();
    }, [service]);

    useEffect(() => {
        if (customer) {
            setFormData({
                name: customer.name,
                tierId: customer.tierId,
                state: customer.state,
                cidrBlocks: customer.cidrBlocks
            });
        } else {
            setFormData({
                name: '',
                tierId: undefined,
                state: CustomerState.ENFORCEMENT,
                cidrBlocks: undefined
            });
        }
        setError(null);
    }, [open, customer]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | SelectChangeEvent) => {
        const { name, value } = e.target;
        
        if (name === 'tierId') {
            setFormData(prev => ({
                ...prev,
                tierId: value === '' ? undefined : (value),
            }));
        } else {
            setFormData(prev => ({ ...prev, [name]: value }));
        }
    };

    const handleSubmit = async () => {
        setError(null);
        setLoading(true);

        try {
            // Validate required fields
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
                        name='tierId'
                        value={formData.tierId ?? ''}
                        onChange={handleChange}
                        label='Tier'
                    >
                        <MenuItem value=''>
                            <em>None</em>
                        </MenuItem>
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
                        {Object.keys(CustomerState).map(key => (
                            <MenuItem key={key} value={CustomerState[key as keyof typeof CustomerState]}>
                                {key}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <TextField
                    label='CIDR Blocks'
                    name='cidrBlocks'
                    value={formData.cidrBlocks ?? ''}
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
