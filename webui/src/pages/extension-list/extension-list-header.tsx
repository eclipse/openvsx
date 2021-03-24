/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import {
    Box, WithStyles, createStyles, Theme, withStyles, Paper, InputBase,
    Select, MenuItem, Container
} from '@material-ui/core';
import { ExtensionCategory, SortBy, SortOrder } from '../../extension-registry-types';
import ArrowUpwardIcon from '@material-ui/icons/ArrowUpward';
import ArrowDownwardIcon from '@material-ui/icons/ArrowDownward';
import { ExtensionListSearchfield } from './extension-list-searchfield';
import { MainContext } from '../../context';

const headerStyles = (theme: Theme) => createStyles({
    formContainer: {
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        [theme.breakpoints.up('md')]: {
            width: '70%'
        },
        [theme.breakpoints.down('md')]: {
            maxWidth: 500,
        }
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        [theme.breakpoints.up('md')]: {
            flexDirection: 'row',
        }
    },
    resultNumAndSortContainer: {
        display: 'flex',
        justifyContent: 'space-between',
        fontSize: '0.75rem',
        marginTop: 5
    },
    resultNum: {
        color: theme.palette.text.hint,
    },
    resultSort: {
        color: theme.palette.text.secondary,
        display: 'flex'
    },
    resultSortBySelectRoot: {
        fontSize: '0.75rem',
        height: '1.1rem'
    },
    resultSortBySelect: {
        padding: '0px !important',
        '&:hover': {
            color: theme.palette.secondary.main
        }
    },
    resultSortBySelectIcon: {
        display: 'none'
    },
    resultSortOrder: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        marginLeft: theme.spacing(0.75),
        '&:hover': {
            cursor: 'pointer',
            color: theme.palette.secondary.main
        }
    },
    category: {
        flex: 1,
        display: 'flex'
    },
    inputBase: {
        flex: 1,
        paddingLeft: theme.spacing(1)
    },
    placeholder: {
        opacity: 0.4
    }
});

class ExtensionListHeaderComp extends React.Component<ExtensionListHeaderComp.Props, ExtensionListHeaderComp.State> {

    static contextType = MainContext;
    declare context: MainContext;

    protected categories: ExtensionCategory[] = [];

    constructor(props: ExtensionListHeaderComp.Props) {
        super(props);
        this.state = {
            category: '',
            sortBy: 'relevance',
            sortOrder: 'desc'
        };
    }

    componentDidMount() {
        this.categories = Array.from(this.context.service.getCategories());
        this.categories.sort((a, b) => {
            if (a === b)
                return 0;
            if (a === 'Other')
                return 1;
            if (b === 'Other')
                return -1;
            return a.localeCompare(b);
        });

        this.setState({
            category: this.props.category || '',
            sortBy: this.props.sortBy,
            sortOrder: this.props.sortOrder
        });
    }

    componentDidUpdate(prevProps: ExtensionListHeaderComp.Props) {
        if (this.props.category !== prevProps.category || this.props.sortBy !== prevProps.sortBy || this.props.sortOrder !== prevProps.sortOrder) {
            this.setState({
                category: this.props.category || '',
                sortBy: this.props.sortBy,
                sortOrder: this.props.sortOrder
            });
        }
    }

    protected handleCategoryChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const category = event.target.value as ExtensionCategory;
        this.setState({ category });
        this.props.onCategoryChanged(category);
    };

    protected handleSearchChange = (value: string) => {
        this.props.onSearchChanged(value);
    };

    protected handleSearchSubmit = (value: string) => {
        this.props.onSearchSubmit(value);
    };

    protected renderValue = (value: string) => {
        if (value === '') {
            return <Box component='span' className={this.props.classes.placeholder}>All Categories</Box>;
        } else {
            return value;
        }
    };

    protected handleSortByChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const sortBy = event.target.value as SortBy;
        this.setState({ sortBy });
        this.props.onSortByChanged(sortBy);
    };

    protected handleSortOrderChange = () => {
        const sortOrder = this.state.sortOrder === 'asc' ? 'desc' : 'asc';
        this.setState({ sortOrder });
        this.props.onSortOrderChanged(sortOrder);
    };

    render() {
        const classes = this.props.classes;
        const SearchHeader = this.context.pageSettings.elements.searchHeader;
        return <React.Fragment>
            <Container>
                <Box display='flex' flexDirection='column' alignItems='center' py={6}>
                    {SearchHeader ? <SearchHeader /> : ''}
                    <Box className={classes.formContainer}>
                        <Box className={classes.form}>
                            <ExtensionListSearchfield
                                onSearchChanged={this.handleSearchChange}
                                onSearchSubmit={this.handleSearchSubmit}
                                searchQuery={this.props.searchQuery}
                                placeholder='Search by Name, Tag, or Description' />
                            <Paper className={classes.category}>
                                <Select
                                    value={this.state.category}
                                    onChange={this.handleCategoryChange}
                                    renderValue={this.renderValue}
                                    displayEmpty
                                    input={<InputBase className={classes.inputBase}></InputBase>}>
                                    <MenuItem value=''>All Categories</MenuItem>
                                    {this.categories.map(c => {
                                        return <MenuItem value={c} key={c}>{c}</MenuItem>;
                                    })}
                                </Select>
                            </Paper>
                        </Box>
                        <Box className={classes.resultNumAndSortContainer}>
                            <Box className={classes.resultNum} >{`${this.props.resultNumber} Result${this.props.resultNumber !== 1 ? 's' : ''}`}</Box>
                            <Box className={classes.resultSort}>
                                <Box>
                                    Sort by
                                    <Select
                                        style={{ marginLeft: '4px' }}
                                        classes={{ root: classes.resultSortBySelectRoot, select: classes.resultSortBySelect, icon: classes.resultSortBySelectIcon }}
                                        disableUnderline
                                        IconComponent={() => <span />}
                                        value={this.state.sortBy}
                                        onChange={this.handleSortByChange}
                                    >
                                        <MenuItem value={'relevance'}>Relevance</MenuItem>
                                        <MenuItem value={'timestamp'}>Date</MenuItem>
                                        <MenuItem value={'downloadCount'}>Downloads</MenuItem>
                                        <MenuItem value={'averageRating'}>Rating</MenuItem>
                                    </Select>
                                </Box>
                                <Box
                                    className={classes.resultSortOrder}
                                    title={this.state.sortOrder === 'asc' ? 'Ascending' : 'Descending'}
                                    onClick={this.handleSortOrderChange}>
                                    {
                                        this.state.sortOrder === 'asc' ?
                                            <ArrowUpwardIcon fontSize='small' />
                                            :
                                            <ArrowDownwardIcon fontSize='small' />
                                    }
                                </Box>
                            </Box>
                        </Box>
                    </Box>
                </Box>
            </Container>
        </React.Fragment>;
    }
}

namespace ExtensionListHeaderComp {
    export interface Props extends WithStyles<typeof headerStyles> {
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
    export interface State {
        category: ExtensionCategory | '';
        sortBy: SortBy;
        sortOrder: SortOrder;
    }
}

export const ExtensionListHeader = withStyles(headerStyles)(ExtensionListHeaderComp);