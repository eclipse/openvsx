/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, KeyboardEvent, useContext } from 'react';
import SearchIcon from '@mui/icons-material/Search';
import { Paper, IconButton, InputBase } from '@mui/material';
import { MainContext } from '../../context';

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

    const { pageSettings } = useContext(MainContext);

    const handleSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
        props.onSearchChanged(event.target.value);
    };

    const handleSearchButtonClick = () => {
        if (props.onSearchSubmit) {
            props.onSearchSubmit(props.searchQuery || '');
        }
    };

    const searchIconColor = pageSettings?.themeType === 'dark' ? '#111111' : '#ffffff';
    return (<>
        <Paper
            sx={{
                flex: 2,
                display: 'flex',
                mr: { xs: 0, sm: 0, md: 1, lg: 1, xl: 1 },
                mb: { xs: 2, sm: 2, md: 0, lg: 0, xl: 0 },
                border: (props.error ? 2 : 0),
                borderColor: 'error.main'
            }}
        >
            <InputBase
                autoFocus={props.autoFocus !== undefined ? props.autoFocus : true}
                value={props.searchQuery}
                onChange={handleSearchChange}
                sx={{ flex: 1, pl: 1 }}
                placeholder={props.placeholder}
                id='search-input'
                type='search'
                inputMode='search'
                onKeyDown={(e: KeyboardEvent) => {
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
                    <IconButton
                        color='primary'
                        aria-label='Search'
                        onClick={handleSearchButtonClick}
                        sx={{
                            bgcolor: 'secondary.main',
                            borderRadius: '0 4px 4px 0',
                            p: 1,
                            transition: 'all 0s',
                            '&:hover': {
                                filter: 'invert(100%)'
                            }
                        }}
                    >
                        <SearchIcon sx={{ color: searchIconColor }} />
                    </IconButton>
            }
        </Paper>
    </>);
};