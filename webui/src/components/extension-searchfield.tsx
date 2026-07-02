/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ChangeEvent, ForwardedRef, forwardRef, KeyboardEvent, useCallback, useRef } from 'react';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Close';
import { IconButton, InputBase, Box } from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { MONO_FONT } from '../default/theme';

interface ExtensionSearchfieldProps {
    onSearchChanged: (s: string) => void;
    onSearchSubmit?: (s: string) => void;
    searchQuery?: string;
    placeholder: string;
    hideIconButton?: boolean;
    error?: boolean;
    autoFocus?: boolean;
    viewTransitionName?: string;
    inputProps?: object;
}

const SearchWrap = styled(Box, {
    shouldForwardProp: prop => prop !== 'hasError'
})<{ hasError?: boolean }>(({ theme, hasError }) => ({
    flex: 2,
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    border: hasError ? '2px solid' : '1px solid',
    borderColor: hasError ? theme.palette.error.main : theme.palette.divider,
    borderRadius: '11px',
    height: '45px',
    padding: '0 13px',
    backgroundColor: theme.palette.surface2,
    transition: 'border-color 0.18s, box-shadow 0.18s',
    '&:focus-within': {
        borderColor: theme.palette.secondary.main,
        boxShadow: `0 0 0 3px ${alpha(theme.palette.secondary.main, 0.16)}`
    }
}));

const MonoSlash = styled('span')(({ theme }) => ({
    fontFamily: MONO_FONT,
    color: theme.palette.secondary.light,
    fontSize: '17px',
    lineHeight: 1,
    flexShrink: 0,
    userSelect: 'none'
}));

const SearchInput = styled(InputBase)(({ theme }) => ({
    flex: 1,
    fontFamily: MONO_FONT,
    fontSize: '15px',
    color: theme.palette.text.primary,
    '& input::placeholder': { color: theme.palette.text.disabled, opacity: 1 },
    '& input::-webkit-search-cancel-button': { display: 'none' }
}));

export const ExtensionSearchfield = forwardRef(
    (props: ExtensionSearchfieldProps, ref: ForwardedRef<HTMLInputElement>) => {
        const inputRef = useRef<HTMLInputElement | null>(null);

        // Keep the forwarded ref and the internal ref (used by the clear button) in sync.
        const setInputRef = useCallback(
            (node: HTMLInputElement | null) => {
                inputRef.current = node;
                if (typeof ref === 'function') {
                    ref(node);
                } else if (ref) {
                    ref.current = node;
                }
            },
            [ref]
        );

        const handleSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
            props.onSearchChanged(event.target.value);
        };

        const handleSearchButtonClick = () => {
            if (props.onSearchSubmit) {
                props.onSearchSubmit(props.searchQuery ?? '');
            }
        };

        const handleClear = () => {
            props.onSearchChanged('');
            inputRef.current?.focus();
        };

        return (
            <SearchWrap
                hasError={props.error}
                style={props.viewTransitionName ? { viewTransitionName: props.viewTransitionName } : undefined}>
                <MonoSlash>/</MonoSlash>
                <SearchInput
                    inputRef={setInputRef}
                    autoFocus={props.autoFocus ?? true}
                    value={props.searchQuery}
                    onChange={handleSearchChange}
                    placeholder={props.placeholder}
                    id='search-input'
                    type='search'
                    inputMode='search'
                    inputProps={props.inputProps}
                    onKeyDown={(e: KeyboardEvent) => {
                        if (e.key === 'Enter' && props.onSearchSubmit) {
                            props.onSearchSubmit(props.searchQuery ?? '');
                        }
                    }}
                />
                <label htmlFor='search-input' className='visually-hidden'>
                    Search for Name, Tags or Description
                </label>
                {props.searchQuery && (
                    <IconButton
                        aria-label='Clear search'
                        onClick={handleClear}
                        size='small'
                        sx={{
                            color: 'text.disabled',
                            p: '4px',
                            flexShrink: 0,
                            transition: 'color 0.14s',
                            '&:hover': { color: 'text.primary' }
                        }}>
                        <ClearIcon sx={{ fontSize: '18px' }} />
                    </IconButton>
                )}
                {!props.hideIconButton && (
                    <IconButton
                        color='primary'
                        aria-label='Search'
                        onClick={handleSearchButtonClick}
                        sx={{
                            bgcolor: 'secondary.main',
                            color: 'secondary.contrastText',
                            borderRadius: '8px',
                            p: '8px',
                            flexShrink: 0,
                            transition: 'background 0.14s',
                            '&:hover': { bgcolor: 'secondary.dark' }
                        }}>
                        <SearchIcon fontSize='small' />
                    </IconButton>
                )}
            </SearchWrap>
        );
    }
);

ExtensionSearchfield.displayName = 'ExtensionSearchfield';
