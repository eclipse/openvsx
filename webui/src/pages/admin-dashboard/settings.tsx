/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ChangeEvent, FC, useCallback, useEffect, useRef, useState } from 'react';
import CheckIcon from '@mui/icons-material/Check';
import SaveIcon from '@mui/icons-material/Save';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Paper,
    Stack,
    Typography,
} from '@mui/material';
import type { Settings } from '../../extension-registry-types';
import { handleError } from '../../utils';
import { SettingsItem } from './settings-item';
import { useSettings, useUpdateSettings } from './use-settings';

interface NotificationState {
    id: string;
    message: string;
    severity: 'error';
    timeout: ReturnType<typeof setTimeout>;
}

const NOTIFICATION_TIMEOUT = 2000;

const SETTINGS: Record<keyof Settings, { title: string; description: string }> = {
    readOnly: {
        title: 'Read-only mode',
        description: 'Blocks write operations while keeping browsing, search, and downloads available.',
    },
};

export const RuntimeSettingsPage: FC = () => {
    const { data: settings, isLoading: loading, error: loadError } = useSettings();
    const { mutate: saveSettings, isPending: saving } = useUpdateSettings();

    const [draftSettings, setDraftSettings] = useState<Settings | null>(null);
    const [errorDismissed, setErrorDismissed] = useState(false);
    const [notifications, setNotifications] = useState<NotificationState[]>([]);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [saveSuccess, setSaveSuccess] = useState(false);
    const saveSuccessTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Keep the editable draft in sync with the loaded (and freshly saved) settings.
    useEffect(() => {
        if (settings) {
            setDraftSettings(settings);
        }
    }, [settings]);

    // A fresh load error should be shown again even if a previous one was dismissed.
    useEffect(() => {
        setErrorDismissed(false);
    }, [loadError]);

    useEffect(() => () => {
        notifications.forEach(n => clearTimeout(n.timeout));
    }, []);

    useEffect(() => () => {
        if (saveSuccessTimer.current) clearTimeout(saveSuccessTimer.current);
    }, []);

    const error = loadError && !errorDismissed ? handleError(loadError as Error) : null;

    const addNotification = useCallback((notification: Pick<NotificationState, 'message'>) => {
        const id = crypto.randomUUID();
        const timeout = setTimeout(() => {
            setNotifications(current => current.filter(n => n.id !== id));
        }, NOTIFICATION_TIMEOUT);
        setNotifications(current => [...current, { ...notification, severity: 'error', id, timeout }]);
    }, []);

    const handleNotificationClose = (id: string) => {
        setNotifications(current => {
            const notification = current.find(n => n.id === id);
            if (notification) clearTimeout(notification.timeout);
            return current.filter(n => n.id !== id);
        });
    };

    const handleFlagChange = useCallback((key: keyof Settings) => (_event: ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setDraftSettings(current => current ? { ...current, [key]: checked } : current);
        setSaveSuccess(false);
    }, []);

    const hasChanges = draftSettings !== null && settings != null &&
        (Object.keys(SETTINGS) as (keyof Settings)[]).some(k => draftSettings[k] !== settings[k]);

    const handleSaveClick = () => setConfirmOpen(true);

    const handleConfirmClose = () => setConfirmOpen(false);

    const handleConfirmSave = useCallback(() => {
        if (!draftSettings) return;
        setConfirmOpen(false);
        saveSettings(draftSettings, {
            onSuccess: () => {
                setSaveSuccess(true);
                if (saveSuccessTimer.current) clearTimeout(saveSuccessTimer.current);
                saveSuccessTimer.current = setTimeout(() => setSaveSuccess(false), 2000);
            },
            onError: (err) => {
                addNotification({
                    message: `Failed to save runtime settings. ${handleError(err as Error)}`,
                });
            },
        });
    }, [draftSettings, saveSettings, addNotification]);

    return (
        <>
            <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box>
                    <Typography variant='h4' component='h1' gutterBottom>
                        Settings
                    </Typography>
                    <Typography variant='body1' color='text.secondary'>
                        Manage runtime settings that apply across the registry.
                    </Typography>
                </Box>

                {error && (
                    <Alert severity='error' onClose={() => setErrorDismissed(true)}>
                        {error}
                    </Alert>
                )}

                <Paper variant='outlined' elevation={0} sx={{ overflow: 'hidden', borderColor: hasChanges ? 'red' : 'grey' }}>
                    {(Object.entries(SETTINGS) as [keyof Settings, { title: string; description: string }][]).map(([key, flag]) => (
                        <SettingsItem
                            key={key}
                            title={flag.title}
                            description={flag.description}
                            checked={draftSettings?.[key] ?? false}
                            loading={loading || !draftSettings}
                            disabled={loading || saving || !draftSettings}
                            onChange={handleFlagChange(key)}
                        />
                    ))}
                </Paper>

                <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        variant='contained'
                        size='large'
                        disabled={!hasChanges || saving || saveSuccess}
                        onClick={handleSaveClick}
                        startIcon={saveSuccess ? <CheckIcon /> : <SaveIcon />}
                        sx={{
                            transition: 'background-color 0.5s ease',
                            ...(saveSuccess && {
                                backgroundColor: 'success.main',
                                '&:hover': { backgroundColor: 'success.dark' },
                                '&.Mui-disabled': { backgroundColor: 'success.main', color: 'white', opacity: 0.9 },
                            }),
                        }}
                    >
                        {saveSuccess ? 'Saved' : 'Save'}
                    </Button>
                </Box>

            </Box>

            <Dialog open={confirmOpen} onClose={handleConfirmClose} maxWidth='xs' fullWidth>
                <DialogTitle>Apply settings?</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        These changes will be applied <strong>immediately</strong> and will affect all users of the registry.
                        Make sure you understand the impact before proceeding.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleConfirmClose}>Cancel</Button>
                    <Button variant='contained' onClick={handleConfirmSave} autoFocus>
                        Apply
                    </Button>
                </DialogActions>
            </Dialog>

            {notifications.length > 0 && (
                <Stack
                    spacing={1.5}
                    sx={{
                        position: 'fixed',
                        right: 24,
                        bottom: 24,
                        zIndex: theme => theme.zIndex.snackbar,
                        width: 'min(420px, calc(100vw - 32px))',
                    }}
                >
                    {notifications.map(notification => (
                    <Alert
                        key={notification.id}
                        onClose={() => handleNotificationClose(notification.id)}
                        severity={notification.severity}
                        variant='filled'
                        sx={{ width: '100%' }}
                    >
                        {notification.message}
                    </Alert>
                    ))}
                </Stack>
            )}
        </>
    );
};