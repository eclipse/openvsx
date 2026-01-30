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

import React, { FunctionComponent, useState } from 'react';
import { Box, Typography, Button, Checkbox, FormControlLabel, Select, MenuItem, SelectChangeEvent, ToggleButtonGroup, ToggleButton, Menu, Badge } from '@mui/material';
import { FilterList as FilterListIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';

interface CountDisplay {
    label: string;
    value: number;
    color?: string;
}

interface FilterOption {
    label: string;
    value: string;
    checked: boolean;
}

interface CountsToolbarProps {
    // Counts to display
    counts: CountDisplay[];

    // Optional filter button with menu
    filterOptions?: FilterOption[];
    onFilterOptionToggle?: (value: string) => void;

    // Optional date range filter
    dateRange?: 'today' | 'last7days' | 'last30days' | 'last90days' | 'all';
    onDateRangeChange?: (range: 'today' | 'last7days' | 'last30days' | 'last90days' | 'all') => void;

    // Optional enforcement filter
    enforcement?: 'enforced' | 'notEnforced' | 'all';
    onEnforcementChange?: (enforcement: 'enforced' | 'notEnforced' | 'all') => void;
}

export const CountsToolbar: FunctionComponent<CountsToolbarProps> = ({
    counts,
    filterOptions = [],
    onFilterOptionToggle,
    dateRange = 'all',
    onDateRangeChange,
    enforcement = 'all',
    onEnforcementChange,
}) => {
    const theme = useTheme();
    const [filterMenuAnchor, setFilterMenuAnchor] = useState<null | HTMLElement>(null);

    const filterMenuOpen = Boolean(filterMenuAnchor);
    const activeFilterCount = filterOptions.filter(opt => opt.checked).length;

    if (counts.length === 0) {
        return null;
    }

    return (
        <Box sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 3,
                height: '48px',
                px: 1.5,
                py: 1.5,
                backgroundColor: theme.palette.scanBackground.dark,
                borderRadius: 1,
                border: `1px solid ${theme.palette.scanBackground.default}`,
                borderColor: 'divider',
                overflow: 'hidden',
            }}>
                {counts.map((count, index) => (
                    <Box
                        key={index}
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.5,
                            flexShrink: 0,
                        }}
                    >
                        <Typography
                            variant='body2'
                            sx={{
                                color: 'text.secondary',
                                fontSize: '0.875rem',
                                fontWeight: 400,
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {count.label}:
                        </Typography>
                        <Typography
                            variant='h6'
                            sx={{
                                color: count.color || 'text.primary',
                                fontSize: '1.25rem',
                                fontWeight: 600,
                                lineHeight: 1,
                            }}
                        >
                            {count.value}
                        </Typography>
                    </Box>
                ))}

                {/* Right-aligned controls */}
                <Box sx={{ ml: 'auto', display: 'flex', alignItems: 'center', gap: 2, flexShrink: 0 }}>
                    {/* Filter Button with Menu */}
                    {onFilterOptionToggle && (
                        <>
                            <Button
                                variant='outlined'
                                size='small'
                                onClick={(e) => setFilterMenuAnchor(e.currentTarget)}
                                disabled={filterOptions.length === 0}
                                sx={{
                                    minWidth: 'auto',
                                    width: '32px',
                                    height: '32px',
                                    p: 0,
                                    borderColor: 'divider',
                                    color: 'text.secondary',
                                    '&:hover': {
                                        borderColor: theme.palette.secondary.main,
                                        backgroundColor: 'transparent',
                                    },
                                }}
                            >
                                <Badge
                                    badgeContent={activeFilterCount}
                                    color='secondary'
                                    invisible={activeFilterCount === 0}
                                >
                                    <FilterListIcon fontSize='small' />
                                </Badge>
                            </Button>
                            <Menu
                                anchorEl={filterMenuAnchor}
                                open={filterMenuOpen}
                                onClose={() => setFilterMenuAnchor(null)}
                                PaperProps={{
                                    sx: {
                                        maxHeight: 400,
                                        minWidth: 200,
                                        backgroundColor: theme.palette.scanBackground.dark,
                                    },
                                }}
                            >
                                {filterOptions.map((option) => (
                                    <MenuItem
                                        key={option.value}
                                        onClick={() => onFilterOptionToggle(option.value)}
                                        sx={{
                                            py: 0.5,
                                            '&:hover': {
                                                backgroundColor: theme.palette.scanBackground.default,
                                            },
                                        }}
                                    >
                                        <FormControlLabel
                                            control={
                                                <Checkbox
                                                    checked={option.checked}
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
                                                    {option.label}
                                                </Typography>
                                            }
                                            sx={{ m: 0, width: '100%', pointerEvents: 'none' }}
                                        />
                                    </MenuItem>
                                ))}
                            </Menu>
                        </>
                    )}

                    {/* Enforcement Filter */}
                    {onEnforcementChange && (
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <ToggleButtonGroup
                                value={enforcement}
                                exclusive
                                onChange={(e, value) => value && onEnforcementChange(value)}
                                size='small'
                                sx={{
                                    height: '32px',
                                    '& .MuiToggleButton-root': {
                                        py: 0.5,
                                        px: 1.5,
                                        fontSize: '0.8125rem',
                                        textTransform: 'none',
                                        whiteSpace: 'nowrap',
                                        borderColor: 'divider',
                                        '&.Mui-selected': {
                                            backgroundColor: theme.palette.secondary.main,
                                            color: theme.palette.secondary.contrastText,
                                            '&:hover': {
                                                backgroundColor: theme.palette.secondary.dark,
                                            },
                                        },
                                    },
                                }}
                            >
                                <ToggleButton value='enforced'>Enforced</ToggleButton>
                                <ToggleButton value='notEnforced'>Not Enforced</ToggleButton>
                                <ToggleButton value='all'>All</ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                    )}

                    {/* Date Range Filter */}
                    {onDateRangeChange && (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexShrink: 0 }}>
                            <Typography variant='body2' sx={{ color: 'text.secondary', fontSize: '0.875rem', whiteSpace: 'nowrap' }}>
                                Date Range:
                            </Typography>
                            <Select
                                value={dateRange}
                                onChange={(e: SelectChangeEvent) => onDateRangeChange(e.target.value as any)}
                                size='small'
                                sx={{
                                    minWidth: 120,
                                    height: '32px',
                                    '& .MuiOutlinedInput-notchedOutline': {
                                        borderColor: 'divider',
                                    },
                                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                        borderColor: theme.palette.secondary.main,
                                    },
                                }}
                            >
                                <MenuItem value='today'>Today</MenuItem>
                                <MenuItem value='last7days'>Last 7 days</MenuItem>
                                <MenuItem value='last30days'>Last 30 days</MenuItem>
                                <MenuItem value='last90days'>Last 90 days</MenuItem>
                                <MenuItem value='all'>All</MenuItem>
                            </Select>
                        </Box>
                    )}
                </Box>
        </Box>
    );
};
