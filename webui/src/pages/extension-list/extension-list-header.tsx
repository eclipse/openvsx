/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, KeyboardEvent, useContext, useEffect, useState } from 'react';
import { Box, Paper, InputBase, Select, MenuItem, Container, SelectChangeEvent } from '@mui/material';
import { ExtensionCategory, SortBy, SortOrder } from '../../extension-registry-types';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import { ExtensionListSearchfield } from './extension-list-searchfield';
import { MainContext } from '../../context';

export const ExtensionListHeader: FunctionComponent<ExtensionListHeaderProps> = props => {
    const [categories, setCategories] = useState<ExtensionCategory[]>([]);
    const [category, setCategory] = useState<ExtensionCategory | ''>('');
    const [sortBy, setSortBy] = useState<SortBy>('relevance');
    const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
    const context = useContext(MainContext);

    useEffect(() => {
        const categories = Array.from(context.service.getCategories());
        categories.sort((a, b) => {
            if (a === b)
                return 0;
            if (a === 'Other')
                return 1;
            if (b === 'Other')
                return -1;
            return a.localeCompare(b);
        });

        setCategories(categories);
        setCategory(props.category || '');
        setSortBy(props.sortBy);
        setSortOrder(props.sortOrder);
    }, []);

    useEffect(() => {
        setCategory(props.category || '');
        setSortBy(props.sortBy);
        setSortOrder(props.sortOrder);
    }, [props.category, props.sortBy, props.sortOrder]);

    const handleCategoryChange = (event: SelectChangeEvent<string>) => {
        const category = event.target.value as ExtensionCategory || '';
        setCategory(category);
        props.onCategoryChanged(category);
    };

    const handleSearchChange = (value: string) => {
        props.onSearchChanged(value);
    };

    const handleSearchSubmit = (value: string) => {
        props.onSearchSubmit(value);
    };

    const handleSortByChange = (event: ChangeEvent<HTMLInputElement>) => {
        const sortBy = event.target.value as SortBy;
        setSortBy(sortBy);
        props.onSortByChanged(sortBy);
    };

    const handleSortOrderChange = () => {
        const newSortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
        setSortOrder(newSortOrder);
        props.onSortOrderChanged(newSortOrder);
    };

    const renderValue = (value: string) => {
        return value === ''
            ? <Box component='span' sx={{ opacity: 0.4 }}>All Categories</Box>
            : value;
    };

    const SearchHeader = context.pageSettings.elements.searchHeader;
    return <>
        <Container>
            <Box display='flex' flexDirection='column' alignItems='center' py={6}>
                {SearchHeader ? <SearchHeader /> : ''}
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        width: { xs: '100%', sm: '100%', md: '70%', lg: '70%', xl: '70%' },
                        maxWidth: { xs: 500, sm: 500, md: 500, lg: 'none', xl: 'none' }
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            width: '100%',
                            flexDirection: { xs: 'column', sm: 'column', md: 'row', lg: 'row', xl: 'row' }
                        }}
                    >
                        <ExtensionListSearchfield
                            onSearchChanged={handleSearchChange}
                            onSearchSubmit={handleSearchSubmit}
                            searchQuery={props.searchQuery}
                            placeholder='Search by Name, Tag, or Description' />
                        <Paper sx={{ flex: 1, display: 'flex' }}>
                            <Select
                                value={category}
                                onChange={handleCategoryChange}
                                renderValue={renderValue}
                                displayEmpty
                                input={<InputBase sx={{ flex: 1, pl: 1 }} />}>
                                <MenuItem value=''>All Categories</MenuItem>
                                {categories.map(c => {
                                    return <MenuItem value={c} key={c}>{c}</MenuItem>;
                                })}
                            </Select>
                        </Paper>
                    </Box>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            fontSize: '0.75rem',
                            mt: 1
                        }}
                    >
                        <Box sx={{ color: 'text.secondary' }} >{`${props.resultNumber} Result${props.resultNumber !== 1 ? 's' : ''}`}</Box>
                        <Box sx={{ color: 'text.secondary', display: 'flex' }}>
                            <Box>
                                Sort by
                                <Select
                                    sx={{
                                        ml: '4px',
                                        fontSize: '0.75rem',
                                        height: '1.1rem',
                                        '& .MuiSelect-select': {
                                            padding: '0px !important',
                                            '&:hover': {
                                                color: 'secondary.main'
                                            }
                                        },
                                        '& .MuiSelect-icon': {
                                            display: 'none'
                                        },
                                        '&.Mui-focused': {
                                            '& .MuiOutlinedInput-notchedOutline': {
                                                border: 0
                                            }
                                        },
                                        '& .MuiOutlinedInput-notchedOutline': {
                                            border: 0
                                        }
                                    }}
                                    IconComponent={() => <span />}
                                    value={sortBy}
                                    onChange={handleSortByChange}
                                >
                                    <MenuItem value={'relevance'}>Relevance</MenuItem>
                                    <MenuItem value={'timestamp'}>Date</MenuItem>
                                    <MenuItem value={'downloadCount'}>Downloads</MenuItem>
                                    <MenuItem value={'rating'}>Rating</MenuItem>
                                </Select>
                            </Box>
                            <Box
                                sx={{
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                    ml: 0.75,
                                    '&:hover': {
                                        cursor: 'pointer',
                                        color: 'secondary.main'
                                    }
                                }}
                                title={sortOrder === 'asc' ? 'Ascending' : 'Descending'}
                                tabIndex={0}
                                onKeyDown={(e: KeyboardEvent) => {
                                    if (e.key === 'Enter') {
                                        e.preventDefault();
                                        handleSortOrderChange();
                                    }
                                }}
                                onClick={handleSortOrderChange}>
                                {
                                    sortOrder === 'asc'
                                        ? <ArrowUpwardIcon fontSize='small' />
                                        : <ArrowDownwardIcon fontSize='small' />
                                }
                            </Box>
                        </Box>
                    </Box>
                </Box>
            </Box>
        </Container>
    </>;
};

export interface ExtensionListHeaderProps {
    onSearchChanged: (s: string) => void;
    onSearchSubmit: (s: string) => void;
    onCategoryChanged: (c: ExtensionCategory) => void;
    onSortByChanged: (sb: SortBy) => void;
    onSortOrderChanged: (so: SortOrder) => void;
    searchQuery?: string;
    category?: ExtensionCategory | '';
    sortBy: SortBy,
    sortOrder: SortOrder,
    resultNumber: number;
}