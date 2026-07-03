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

import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { Extension } from '../../extension-registry-types';

export const ExtensionHeader: FunctionComponent<ExtensionHeaderProps> = ({ extension, icon, defaultIcon }) => (
    <Box display='flex' alignItems='center' gap={2} mb={2}>
        <Box
            component='img'
            src={icon ?? defaultIcon ?? ''}
            alt={extension.displayName ?? extension.name}
            sx={{ width: '4rem', height: '4rem', objectFit: 'contain' }}
        />
        <Box>
            <Typography variant='h5'>{extension.displayName ?? extension.name}</Typography>
            <Typography variant='body2' color='text.secondary'>
                {extension.namespace}.{extension.name} &middot; v{extension.version}
            </Typography>
        </Box>
    </Box>
);

export interface ExtensionHeaderProps {
    extension: Extension;
    icon: string | undefined;
    defaultIcon?: string;
}
