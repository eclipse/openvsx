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

import { ChangeEvent, FC, useCallback, useContext, useEffect, useRef, useState } from 'react';
import {
    Alert,
    Box,
    Divider,
    Paper,
    Stack,
    Typography,
} from '@mui/material';
import { MainContext } from '../../context';
import type { RuntimeFeatureFlags } from '../../extension-registry-types';
import { handleError } from '../../utils';
import { RuntimeFeatureFlagItem } from './runtime-feature-flag-item';

interface NotificationState {
    id: string;
    message: string;
    severity: 'success' | 'error';
    timeout: ReturnType<typeof setTimeout>;
}

const NOTIFICATION_TIMEOUT = 5000;

const FEATURE_FLAGS: Record<keyof RuntimeFeatureFlags, { title: string; description: string }> = {
    readOnlyMode: {
        title: 'Read-only mode',
        description: 'Blocks write operations while keeping browsing, search, and downloads available.',
    },
};

export const RuntimeFeatureFlagsPage: FC = () => {
    const abortController = useRef<AbortController>(new AbortController());
    const { service } = useContext(MainContext);

    const [featureFlags, setFeatureFlags] = useState<RuntimeFeatureFlags | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notifications, setNotifications] = useState<NotificationState[]>([]);

    useEffect(() => {
        return () => abortController.current.abort();
    }, []);

    const loadRuntimeFeatureFlags = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await service.admin.getRuntimeFeatureFlags(abortController.current);
            setFeatureFlags(data);
        } catch (err) {
            setError(handleError(err as Error));
        } finally {
            setLoading(false);
        }
    }, [service]);

    useEffect(() => {
        loadRuntimeFeatureFlags();
    }, [loadRuntimeFeatureFlags]);

    useEffect(() => () => {
        notifications.forEach(n => clearTimeout(n.timeout));
    }, []);

    const addNotification = useCallback((notification: Pick<NotificationState, 'message' | 'severity'>) => {
        const id = crypto.randomUUID();
        const timeout = setTimeout(() => {
            setNotifications(current => current.filter(n => n.id !== id));
        }, NOTIFICATION_TIMEOUT);
        setNotifications(current => [...current, { ...notification, id, timeout }]);
    }, []);

    const handleNotificationClose = (id: string) => {
        setNotifications(current => {
            const notification = current.find(n => n.id === id);
            if (notification) clearTimeout(notification.timeout);
            return current.filter(n => n.id !== id);
        });
    };

    const handleFlagChange = useCallback((key: keyof RuntimeFeatureFlags) => async (_event: ChangeEvent<HTMLInputElement>, checked: boolean) => {
        if (!featureFlags || saving) return;

        const previousFeatureFlags = featureFlags;
        const nextFeatureFlags: RuntimeFeatureFlags = { ...featureFlags, [key]: checked };

        setFeatureFlags(nextFeatureFlags);
        setSaving(true);
        setError(null);

        try {
            const updatedFeatureFlags = await service.admin.updateRuntimeFeatureFlags(abortController.current, nextFeatureFlags);
            setFeatureFlags(updatedFeatureFlags);
            addNotification({ severity: 'success', message: 'Runtime feature flags saved.' });
        } catch (err) {
            setFeatureFlags(previousFeatureFlags);
            addNotification({
                severity: 'error',
                message: `Failed to save runtime feature flags. ${handleError(err as Error)}`,
            });
        } finally {
            setSaving(false);
        }
    }, [featureFlags, saving, service, addNotification]);

    return (
        <>
            <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box>
                    <Typography variant='h4' component='h1' gutterBottom>
                        Settings
                    </Typography>
                    <Typography variant='body1' color='text.secondary'>
                        Manage runtime feature flags that apply across the registry.
                    </Typography>
                </Box>

                {error && (
                    <Alert severity='error' onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}

                <Paper variant='outlined' elevation={0} sx={{ overflow: 'hidden' }}>
                    {(Object.entries(FEATURE_FLAGS) as [keyof RuntimeFeatureFlags, { title: string; description: string }][]).map(([key, flag]) => (
                        <RuntimeFeatureFlagItem
                            key={key}
                            title={flag.title}
                            description={flag.description}
                            checked={featureFlags?.[key] ?? false}
                            loading={loading || !featureFlags}
                            disabled={loading || saving || !featureFlags}
                            onChange={handleFlagChange(key)}
                        />
                    ))}
                </Paper>
            </Box>

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