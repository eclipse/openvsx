/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent } from 'react';
import { Dialog, DialogTitle, DialogContent, Box, Typography, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { KbdKey } from './kbd-key';
import { useKeyboardShortcuts } from '../keyboard-shortcuts-context';

interface ShortcutsModalProps {
    open: boolean;
    onClose: () => void;
}

export const ShortcutsModal: FunctionComponent<ShortcutsModalProps> = ({ open, onClose }) => {
    const { shortcuts } = useKeyboardShortcuts();

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth='xs'
            fullWidth
            PaperProps={{
                elevation: 0,
                sx: {
                    borderRadius: '16px',
                    bgcolor: 'background.paper',
                    border: '1px solid',
                    borderColor: 'divider',
                    boxShadow: 'var(--shadow-lg)'
                }
            }}>
            <DialogTitle
                component='div'
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    py: '14px',
                    px: '20px',
                    borderBottom: '1px solid',
                    borderColor: 'divider'
                }}>
                <Typography sx={{ fontSize: '16px', fontWeight: 700 }}>Keyboard shortcuts</Typography>
                <IconButton
                    onClick={onClose}
                    size='small'
                    edge='end'
                    aria-label='Close'
                    sx={{ color: 'text.disabled' }}>
                    <CloseIcon fontSize='small' />
                </IconButton>
            </DialogTitle>
            <DialogContent sx={{ p: '0 !important' }}>
                {shortcuts.map((s, i) => (
                    <Box
                        key={s.key}
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            px: '20px',
                            py: '11px',
                            borderBottom: i < shortcuts.length - 1 ? '1px solid' : 'none',
                            borderColor: 'border2'
                        }}>
                        <Typography sx={{ fontSize: '14px', color: 'text.secondary' }}>{s.label}</Typography>
                        <KbdKey>{s.key}</KbdKey>
                    </Box>
                ))}
            </DialogContent>
        </Dialog>
    );
};
