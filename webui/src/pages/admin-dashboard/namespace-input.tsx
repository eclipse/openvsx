/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState } from 'react';
import { Paper, IconButton, makeStyles, InputBase } from '@material-ui/core';
import SearchIcon from '@material-ui/icons/Search';

const useStyles = makeStyles(theme =>
    ({
        root: {
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
        error: {
            border: `2px solid ${theme.palette.error.main}`
        }
    })
);

interface InputProps {
    onSubmit?: (inputValue: string) => void;
    onChange: (inputValue: string) => void;
    hideIconButton?: boolean;
    error?: boolean;
    autoFocus?: boolean;
    placeholder?: string;
}

export const StyledInput: FunctionComponent<InputProps> = props => {
    const classes = useStyles();
    const [inputValue, setInputValue] = useState('');
    const onChangeInputValue = (ev: React.ChangeEvent<HTMLInputElement>) => {
        const inputValue = ev.target.value;
        props.onChange(inputValue);
        setInputValue(inputValue);
    };
    const onSubmit = () => {
        if (props.onSubmit) {
            props.onSubmit(inputValue);
        }
    };
    return <Paper className={classes.root} classes={{ root: props.error ? classes.error : '' }}>
        <InputBase
            autoFocus={props.autoFocus !== undefined ? props.autoFocus : true}
            className={classes.input}
            placeholder={props.placeholder}
            onChange={onChangeInputValue}
            onKeyPress={(e: React.KeyboardEvent) => {
                if (e.charCode === 13 && props.onSubmit) {
                    props.onSubmit(inputValue);
                }
            }}
        />
        {
            props.hideIconButton ? '' :
                <IconButton color='primary' type='submit' className={classes.iconButton} onClick={onSubmit}>
                    <SearchIcon />
                </IconButton>
        }
    </Paper>;
};