/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext } from 'react';
import SearchIcon from '@material-ui/icons/Search';
import { Paper, IconButton, makeStyles, InputBase } from '@material-ui/core';
import { MainContext } from '../../context';

const useStyles = makeStyles((theme) => ({
    search: {
        flex: 2,
        display: 'flex',
        marginRight: theme.spacing(1),
        [theme.breakpoints.down('sm')]: {
            marginRight: 0,
            marginBottom: theme.spacing(2),
        },
    },
    input: {
        flex: 1,
        paddingLeft: theme.spacing(1)
    },
    iconButton: {
        backgroundColor: theme.palette.secondary.main,
        borderRadius: '0 4px 4px 0',
        padding: theme.spacing(1),
        transition: 'all 0s',
        '&:hover': {
            filter: 'invert(100%)',
        }
    },
    searchIconLight: {
        color: '#ffffff',
    },
    searchIconDark: {
        color: '#111111',
    },
    error: {
        border: `2px solid ${theme.palette.error.main}`
    }
}));

interface ExtensionListSearchfieldProps {
    onSearchChanged: (s: string) => void;
    onSearchSubmit?: (s: string) => void;
    searchQuery?: string;
    placeholder: string;
    hideIconButton?: boolean;
    error?: boolean;
    autoFocus?: boolean;
}

export const ExtensionListSearchfield: FunctionComponent<ExtensionListSearchfieldProps> = props => {
    const classes = useStyles();

    const { pageSettings } = useContext(MainContext);

    const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        props.onSearchChanged(event.target.value);
    };

    const handleSearchButtonClick = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
        if (props.onSearchSubmit) {
            props.onSearchSubmit(props.searchQuery || '');
        }
    };

    const searchIconClass = pageSettings?.themeType === 'dark' ? classes.searchIconDark : classes.searchIconLight;

    return (<>
        <Paper className={classes.search} classes={{ root: props.error ? classes.error : '' }}>
            <InputBase
                autoFocus={props.autoFocus !== undefined ? props.autoFocus : true}
                value={props.searchQuery || ''}
                onChange={handleSearchChange}
                className={classes.input}
                placeholder={props.placeholder}
                id='search-input'
                type='search'
                inputMode='search'
                onKeyPress={(e: React.KeyboardEvent) => {
                    if (e.key === 'Enter' && props.onSearchSubmit) {
                        props.onSearchSubmit(props.searchQuery || '');
                    }
                }}
            />
            <label
                htmlFor='search-input'
                className='visually-hidden' >
                Search for Name, Tags or Description
                                </label>
            {
                props.hideIconButton ? '' :
                    <IconButton color='primary' aria-label='Search' classes={{ root: classes.iconButton }} onClick={handleSearchButtonClick}>
                        <SearchIcon classes={{ root: searchIconClass }} />
                    </IconButton>
            }
        </Paper>
    </>);
};