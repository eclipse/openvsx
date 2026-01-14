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
import { Box, Typography, TextField, InputAdornment, Button, Checkbox, FormControlLabel } from '@mui/material';
import { CheckCircle as CheckCircleIcon, RadioButtonUnchecked as RadioButtonUncheckedIcon, PersonOutlined as PersonIcon, AccountTreeOutlined as AccountTreeIcon, ExtensionOutlined as ExtensionIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';

interface FilterItem {
    label: string;
    value: string;
    checked: boolean;
    onChange: (value: string) => void;
    disabled?: boolean;
}

interface ActionButton {
    label: string;
    color: string;
    disabled: boolean;
    onClick: () => void;
}

interface SearchToolbarProps {
    publisherQuery: string;
    namespaceQuery: string;
    nameQuery: string;
    onPublisherChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    onNamespaceChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    onNameChange: (event: React.ChangeEvent<HTMLInputElement>) => void;

    // Optional inline filters
    filters?: FilterItem[];

    // Optional select all checkbox
    showSelectAll?: boolean;
    allSelected?: boolean;
    onSelectAllChange?: (checked: boolean) => void;

    // Optional action buttons
    actionButtons?: ActionButton[];

    // Optional selected count display
    selectedCount?: number;
}

export const SearchToolbar: FunctionComponent<SearchToolbarProps> = ({
    publisherQuery,
    namespaceQuery,
    nameQuery,
    onPublisherChange,
    onNamespaceChange,
    onNameChange,
    filters = [],
    showSelectAll = false,
    allSelected = false,
    onSelectAllChange,
    actionButtons = [],
    selectedCount = 0,
}) => {
    const theme = useTheme();

    return (
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', pb: 1.5, minHeight: '56px', overflow: 'hidden' }}>
            <TextField
                placeholder='Publisher'
                value={publisherQuery}
                onChange={onPublisherChange}
                size='small'
                InputProps={{
                    startAdornment: (
                        <InputAdornment position='start'>
                            <PersonIcon sx={{ fontSize: 20 }} />
                        </InputAdornment>
                    ),
                }}
                sx={{
                    flex: 1,
                    minWidth: 100,
                    maxWidth: '16.67%',
                    '& .MuiOutlinedInput-root': {
                        backgroundColor: theme.palette.scanBackground.dark,
                        '&.Mui-focused fieldset': {
                            borderColor: theme.palette.secondary.main,
                        },
                    },
                }}
            />
            <TextField
                placeholder='Namespace'
                value={namespaceQuery}
                onChange={onNamespaceChange}
                size='small'
                InputProps={{
                    startAdornment: (
                        <InputAdornment position='start'>
                            <AccountTreeIcon sx={{ fontSize: 20 }} />
                        </InputAdornment>
                    ),
                }}
                sx={{
                    flex: 1,
                    minWidth: 100,
                    maxWidth: '16.67%',
                    '& .MuiOutlinedInput-root': {
                        backgroundColor: theme.palette.scanBackground.dark,
                        '&.Mui-focused fieldset': {
                            borderColor: theme.palette.secondary.main,
                        },
                    },
                }}
            />
            <TextField
                placeholder='Name or Display Name'
                value={nameQuery}
                onChange={onNameChange}
                size='small'
                InputProps={{
                    startAdornment: (
                        <InputAdornment position='start'>
                            <ExtensionIcon sx={{ fontSize: 20 }} />
                        </InputAdornment>
                    ),
                }}
                sx={{
                    flex: 1,
                    minWidth: 100,
                    maxWidth: '16.67%',
                    '& .MuiOutlinedInput-root': {
                        backgroundColor: theme.palette.scanBackground.dark,
                        '&.Mui-focused fieldset': {
                            borderColor: theme.palette.secondary.main,
                        },
                    },
                }}
            />

            {/* Inline Filter Checkboxes */}
            {filters.length > 0 && (
                <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'nowrap', justifyContent: 'flex-end', flex: 1 }}>
                    {filters.map((filter) => (
                        <FormControlLabel
                            key={filter.value}
                            control={
                                <Checkbox
                                    checked={filter.checked}
                                    onChange={() => filter.onChange(filter.value)}
                                    disabled={filter.disabled}
                                    size='small'
                                    sx={{
                                        color: theme.palette.checkboxUnchecked,
                                        '&.Mui-checked': {
                                            color: theme.palette.secondary.main,
                                        },
                                    }}
                                />
                            }
                            label={
                                <Typography variant='body2' sx={{ fontSize: '0.875rem' }}>
                                    {filter.label}
                                </Typography>
                            }
                            sx={{ mr: 0 }}
                            disabled={filter.disabled}
                        />
                    ))}
                </Box>
            )}

            {/* Select All and Action Buttons */}
            <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', ml: 'auto' }}>
                {showSelectAll && onSelectAllChange && (
                    <Button
                        variant='outlined'
                        size='small'
                        onClick={() => onSelectAllChange(!allSelected)}
                        startIcon={allSelected ? <CheckCircleIcon /> : <RadioButtonUncheckedIcon />}
                        sx={{
                            textTransform: 'none',
                            whiteSpace: 'nowrap',
                            color: allSelected ? 'secondary.main' : 'text.secondary',
                            borderColor: allSelected ? 'secondary.main' : 'divider',
                        }}
                    >
                        Select All
                    </Button>
                )}
            </Box>

            {/* Action Buttons */}
            {actionButtons.length > 0 && (
                <Box sx={{ position: 'relative' }}>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                        {actionButtons.map((button, index) => (
                            <Button
                                key={index}
                                variant='outlined'
                                disabled={button.disabled}
                                onClick={button.onClick}
                                size='small'
                                sx={{
                                    textTransform: 'none',
                                    minWidth: 'auto',
                                    px: 2,
                                    fontWeight: 500,
                                    borderColor: button.color,
                                    color: button.color,
                                    backgroundColor: 'transparent',
                                    '&:hover': {
                                        borderColor: button.color,
                                        backgroundColor: button.color,
                                        color: theme.palette.primary.contrastText,
                                    },
                                    '&:disabled': {
                                        backgroundColor: 'transparent',
                                        color: theme.palette.text.disabled,
                                        borderColor: theme.palette.divider,
                                    },
                                }}
                            >
                                {button.label}
                            </Button>
                        ))}
                    </Box>
                    {selectedCount > 0 && (
                        <Typography
                            variant='body2'
                            sx={{
                                position: 'absolute',
                                right: 0,
                                top: '100%',
                                color: 'text.secondary',
                                fontSize: '0.75rem',
                                mt: 0.5,
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {selectedCount} selected
                        </Typography>
                    )}
                </Box>
            )}
        </Box>
    );
};
