/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, KeyboardEvent } from 'react';
import { Box, Select, MenuItem, Typography, SelectChangeEvent } from '@mui/material';
import { SortBy, SortOrder } from '../../extension-registry-types';
import { ExtensionCategory } from '../../extension-registry-types';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

export const SearchHeader: FunctionComponent<SearchHeaderProps> = props => {
    const { sortBy, sortOrder, onSortByChanged, onSortOrderChanged } = props;

    const handleSortByChange = (event: SelectChangeEvent<string>) => {
        onSortByChanged(event.target.value as SortBy);
    };

    const toggleSortOrder = () => {
        onSortOrderChanged(sortOrder === 'asc' ? 'desc' : 'asc');
    };

    const title = props.searchQuery ? `"${props.searchQuery}"` : props.category || 'All extensions';

    return (
        <Box
            sx={{
                pt: '1.75rem',
                pb: '1rem',
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                gap: '1rem',
                flexWrap: 'wrap'
            }}>
            <Box>
                <Typography
                    component='h1'
                    sx={{
                        fontSize: '1.6rem',
                        fontWeight: 700,
                        letterSpacing: '-0.025em',
                        lineHeight: 1.2,
                        color: 'text.primary'
                    }}>
                    {title}
                </Typography>
                <Typography sx={{ fontSize: '0.8125rem', color: 'text.disabled', mt: '0.3125rem' }}>
                    {props.resultNumber.toLocaleString()} extensions found
                </Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: '0.375rem', flexShrink: 0, mt: '0.375rem' }}>
                <Box component='span' sx={{ fontSize: '0.8125rem', color: 'text.disabled' }}>
                    Sort by
                </Box>
                <Select
                    value={sortBy}
                    onChange={handleSortByChange}
                    size='small'
                    sx={{
                        fontSize: '0.8125rem',
                        fontWeight: 500,
                        color: 'text.primary',
                        height: '1.875rem',
                        bgcolor: 'background.paper',
                        borderRadius: '8px',
                        '& .MuiSelect-icon': { color: 'text.disabled', fontSize: '1.125rem' }
                    }}>
                    <MenuItem value='relevance'>Relevance</MenuItem>
                    <MenuItem value='timestamp'>Date</MenuItem>
                    <MenuItem value='downloadCount'>Downloads</MenuItem>
                    <MenuItem value='rating'>Rating</MenuItem>
                </Select>
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        color: 'text.disabled',
                        borderRadius: '6px',
                        p: '0.1875rem',
                        cursor: 'pointer',
                        transition: 'color 0.14s',
                        '&:hover': { color: 'secondary.light' }
                    }}
                    title={sortOrder === 'asc' ? 'Ascending' : 'Descending'}
                    tabIndex={0}
                    onKeyDown={(e: KeyboardEvent) => {
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            toggleSortOrder();
                        }
                    }}
                    onClick={toggleSortOrder}>
                    {sortOrder === 'asc' ? (
                        <ArrowUpwardIcon fontSize='small' />
                    ) : (
                        <ArrowDownwardIcon fontSize='small' />
                    )}
                </Box>
            </Box>
        </Box>
    );
};

export interface SearchHeaderProps {
    onSortByChanged: (sb: SortBy) => void;
    onSortOrderChanged: (so: SortOrder) => void;
    sortBy: SortBy;
    sortOrder: SortOrder;
    resultNumber: number;
    searchQuery?: string;
    category?: ExtensionCategory | '';
}
