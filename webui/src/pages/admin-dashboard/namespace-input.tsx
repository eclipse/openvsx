/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, KeyboardEvent, useContext, useState } from 'react';
import { Paper, IconButton, InputBase } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { MainContext } from '../../context';

interface InputProps {
    onSubmit?: (inputValue: string) => void;
    onChange: (inputValue: string) => void;
    hideIconButton?: boolean;
    error?: boolean;
    autoFocus?: boolean;
    placeholder?: string;
}

export const StyledInput: FunctionComponent<InputProps> = props => {
    const [inputValue, setInputValue] = useState('');
    const { pageSettings } = useContext(MainContext);
    const onChangeInputValue = (ev: ChangeEvent<HTMLInputElement>) => {
        const inputValue = ev.target.value;
        props.onChange(inputValue);
        setInputValue(inputValue);
    };
    const onSubmit = () => {
        if (props.onSubmit) {
            props.onSubmit(inputValue);
        }
    };

    const searchIconColor = pageSettings?.themeType === 'dark' ? '#111111' : '#ffffff';
    return <Paper
                elevation={3}
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
                    sx={{ flex: 1, pl: 1 }}
                    placeholder={props.placeholder}
                    onChange={onChangeInputValue}
                    onKeyDown={(e: KeyboardEvent) => {
                        if (e.key === 'Enter' && props.onSubmit) {
                            props.onSubmit(inputValue);
                        }
                    }}
                />
                {
                    props.hideIconButton ? '' :
                        <IconButton
                            color='primary'
                            type='submit'
                            onClick={onSubmit}
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
                            <SearchIcon sx={{ color: searchIconColor }}/>
                        </IconButton>
                }
            </Paper>;
};