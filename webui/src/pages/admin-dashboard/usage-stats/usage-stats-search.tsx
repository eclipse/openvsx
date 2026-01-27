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

import React, { FC } from "react";
import { Paper, Autocomplete, InputBase, IconButton } from "@mui/material";
import SearchIcon from '@mui/icons-material/Search';
import type { Customer } from "../../../extension-registry-types";

interface CustomerSearchProps {
    customers: Customer[];
    selectedCustomer: Customer | null;
    loading: boolean;
    onCustomerChange: (_: unknown, value: Customer | null) => void;
    pageSettings?: { themeType?: string };
}

export const CustomerSearch: FC<CustomerSearchProps> = ({
    customers,
    selectedCustomer,
    loading,
    onCustomerChange,
    pageSettings
}) => {
    const searchIconColor = pageSettings?.themeType === 'dark' ? '#111111' : '#ffffff';

    return (
        <Autocomplete
            key='customer-search'
            options={customers}
            getOptionLabel={(option) => option.name}
            value={selectedCustomer}
            onChange={onCustomerChange}
            loading={loading}
            renderInput={(params) => {
                const { ref, color, size, ...inputProps } = params.inputProps;
                return (
                    <Paper
                        ref={params.InputProps.ref}
                        elevation={3}
                        sx={{
                            flex: 2,
                            display: 'flex',
                            mr: { xs: 0, sm: 0, md: 1, lg: 1, xl: 1 },
                            mb: { xs: 2, sm: 2, md: 0, lg: 0, xl: 0 },
                        }}
                    >
                        <InputBase
                            inputRef={ref}
                            {...inputProps}
                            autoFocus
                            sx={{ flex: 1, pl: 1 }}
                            placeholder='Search Customer'
                        />
                        <IconButton
                            color='primary'
                            sx={{
                                bgcolor: 'secondary.main',
                                borderRadius: '0 4px 4px 0',
                                padding: 1,
                                transition: 'all 0s',
                                '&:hover': {
                                    filter: 'invert(100%)',
                                }
                            }}
                        >
                            <SearchIcon sx={{ color: searchIconColor }} />
                        </IconButton>
                    </Paper>
                );
            }}
        />
    );
};