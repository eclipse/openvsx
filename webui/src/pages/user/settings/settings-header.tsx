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

import { FunctionComponent, ReactNode } from 'react';
import { Box, Typography } from '@mui/material';

/**
 * Page heading shared by the settings tabs: title and optional actions above a
 * hairline rule, with an optional description paragraph below it.
 */
export const SettingsHeader: FunctionComponent<SettingsHeaderProps> = ({ title, actions, description }) => (
    <Box sx={{ mb: '1.375rem' }}>
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '1rem',
                flexWrap: 'wrap',
                pb: '1rem',
                borderBottom: '1px solid',
                borderColor: 'divider'
            }}>
            <Typography component='h1' sx={{ fontSize: '1.4375rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
                {title}
            </Typography>
            {actions ? (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>{actions}</Box>
            ) : null}
        </Box>
        {description ? (
            <Typography sx={{ fontSize: '0.875rem', color: 'text.disabled', mt: '1.375rem' }}>{description}</Typography>
        ) : null}
    </Box>
);

export interface SettingsHeaderProps {
    title: string;
    actions?: ReactNode;
    description?: ReactNode;
}
