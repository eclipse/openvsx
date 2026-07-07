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

import { FunctionComponent } from 'react';
import { Dialog, DialogTitle, DialogContent, Box, Typography, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { KbdKey } from './kbd-key';
import { useKeyboardShortcuts } from '../context/keyboard-shortcuts-context';

interface ShortcutsModalProps {
    open: boolean;
    onClose: () => void;
}

export const ShortcutsModal: FunctionComponent<ShortcutsModalProps> = ({ open, onClose }) => {
    const { shortcuts } = useKeyboardShortcuts();

    return (
        <Dialog open={open} onClose={onClose} maxWidth='xs' fullWidth>
            <DialogTitle
                component='div'
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    py: '0.875rem',
                    px: '1.25rem',
                    borderBottom: '1px solid',
                    borderColor: 'divider'
                }}>
                <Typography sx={{ fontSize: '1rem', fontWeight: 700 }}>Keyboard shortcuts</Typography>
                <IconButton
                    onClick={onClose}
                    size='small'
                    edge='end'
                    aria-label='Close'
                    sx={{ color: 'text.disabled' }}>
                    <CloseIcon fontSize='small' />
                </IconButton>
            </DialogTitle>
            {/* This dialog exists to display keys, so it re-shows the globally touch-hidden chips. */}
            <DialogContent sx={{ p: 0, '& kbd': { display: 'inline-block' } }}>
                {shortcuts.map((s, i) => (
                    <Box
                        key={s.key}
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            px: '1.25rem',
                            py: '0.6875rem',
                            borderBottom: i < shortcuts.length - 1 ? '1px solid' : 'none',
                            borderColor: 'border2'
                        }}>
                        <Typography sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>{s.label}</Typography>
                        <KbdKey>{s.key}</KbdKey>
                    </Box>
                ))}
            </DialogContent>
        </Dialog>
    );
};
