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
import { Box, Typography, Switch } from '@mui/material';
import { formatDateTime } from './utils';

interface AutoRefreshProps {
    lastRefreshed: Date | null;
    autoRefresh?: boolean;
    onAutoRefreshChange?: (enabled: boolean) => void;
}

export const AutoRefresh: FunctionComponent<AutoRefreshProps> = ({
    lastRefreshed,
    autoRefresh = false,
    onAutoRefreshChange,
}) => {
    if (!lastRefreshed) {
        return null;
    }

    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: 1,
            }}
        >
            {onAutoRefreshChange && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Typography
                        variant='body2'
                        sx={{
                            color: 'text.secondary',
                            fontSize: '0.75rem',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        30s auto-refresh
                    </Typography>
                    <Switch
                        size='small'
                        checked={autoRefresh}
                        onChange={(e) => onAutoRefreshChange(e.target.checked)}
                        color='secondary'
                        sx={{
                            transform: 'scale(0.7)',
                            marginRight: -0.5,
                        }}
                    />
                </Box>
            )}
            <Typography
                variant='body2'
                sx={{
                    color: 'text.secondary',
                    fontSize: '0.75rem',
                    whiteSpace: 'nowrap',
                    pl: 1,
                }}
            >
                Last Refreshed: {formatDateTime(lastRefreshed.toISOString())}
            </Typography>
        </Box>
    );
};
