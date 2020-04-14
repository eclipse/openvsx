/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React = require("react");
import {
    Typography, Box, WithStyles, createStyles, Theme, withStyles, Paper,
    IconButton, InputBase, Select, MenuItem, Container
} from "@material-ui/core";
import SearchIcon from '@material-ui/icons/Search';
import { ExtensionRegistryService } from "../../extension-registry-service";
import { ExtensionCategory } from "../../extension-registry-types";
import { PageSettings } from "../../page-settings";

const headerStyles = (theme: Theme) => createStyles({
    form: {
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        [theme.breakpoints.up('md')]: {
            flexDirection: 'row',
            width: '70%'
        },
        [theme.breakpoints.down('md')]: {
            maxWidth: 500,
        }
    },
    search: {
        flex: 2,
        display: 'flex',
        marginRight: theme.spacing(1),
        [theme.breakpoints.down('sm')]: {
            marginRight: 0,
            marginBottom: theme.spacing(2),
        },
    },
    category: {
        flex: 1,
        display: 'flex'
    },
    inputBase: {
        flex: 1,
        paddingLeft: theme.spacing(1)
    },
    iconButton: {
        backgroundColor: theme.palette.secondary.main,
        borderRadius: '0 4px 4px 0',
        padding: theme.spacing(1)
    },
    typo: {
        marginBottom: theme.spacing(2),
        fontWeight: theme.typography.fontWeightLight,
        letterSpacing: 4,
        textAlign: 'center'
    },
    placeholder: {
        opacity: 0.4
    }
});

class ExtensionListHeaderComp extends React.Component<ExtensionListHeaderComp.Props, ExtensionListHeaderComp.State> {

    protected categories: ExtensionCategory[];

    constructor(props: ExtensionListHeaderComp.Props) {
        super(props);

        this.categories = this.props.service.getCategories();

        this.state = {
            category: ''
        };
    }

    componentDidMount() {
        this.setState({ category: this.props.category || '' });
    }

    componentDidUpdate(prevProps: ExtensionListHeaderComp.Props) {
        if (this.props.category !== prevProps.category) {
            this.setState({ category: this.props.category || '' });
        }
    }

    protected handleCategoryChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const category = event.target.value as ExtensionCategory;
        this.setState({ category });
        this.props.onCategoryChanged(category);
    }

    protected handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        this.props.onSearchChanged(event.target.value);
    }

    protected renderValue = (value: string) => {
        if (value === '') {
            return <Box component='span' className={this.props.classes.placeholder}>All Categories</Box>;
        } else {
            return value;
        }
    }

    render() {
        const { classes } = this.props;
        return <React.Fragment>
            <Container>
                <Box display='flex' flexDirection='column' alignItems='center' py={6}>
                    <Typography variant='h4' classes={{ root: classes.typo }}>
                        {this.props.pageSettings.listHeaderTitle}
                    </Typography>
                    <Box className={classes.form}>
                        <Paper className={classes.search}>
                            <InputBase
                                autoFocus
                                value={this.props.searchQuery || ''}
                                onChange={this.handleSearchChange}
                                className={classes.inputBase}
                                placeholder='Search for Name, Tags or Description'
                                id="search-input"
                            >
                            </InputBase>
                            <label
                                htmlFor="search-input"
                                className="visually-hidden"
                            >
                                Search for Name, Tags or Description
                            </label>
                            <IconButton color='primary' aria-label="Search" classes={{ root: classes.iconButton }}>
                                <SearchIcon />
                            </IconButton>
                        </Paper>
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
                </Box>
            </Container>
        </React.Fragment>;
    }
}

namespace ExtensionListHeaderComp {
    export interface Props extends WithStyles<typeof headerStyles> {
        onSearchChanged: (s: string) => void;
        onCategoryChanged: (c: ExtensionCategory) => void;
        pageSettings: PageSettings;
        searchQuery?: string;
        category?: ExtensionCategory | '';
        service: ExtensionRegistryService;
    }
    export interface State {
        category: ExtensionCategory | '';
    }
}

export const ExtensionListHeader = withStyles(headerStyles)(ExtensionListHeaderComp);