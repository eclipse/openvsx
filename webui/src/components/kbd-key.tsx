/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, PropsWithChildren } from 'react';
import { Box } from '@mui/material';
import { MONO_FONT } from '../default/theme';

export const KbdKey: FunctionComponent<PropsWithChildren> = ({ children }) => (
    <Box
        component='kbd'
        sx={{
            fontFamily: MONO_FONT,
            fontSize: '11px',
            fontWeight: 600,
            lineHeight: 1,
            px: '6px',
            py: '3px',
            borderRadius: '5px',
            bgcolor: 'surface3',
            border: '1px solid',
            borderColor: 'divider',
            color: 'text.disabled',
            display: 'inline-block',
            userSelect: 'none',
            verticalAlign: 'middle'
        }}>
        {children}
    </Box>
);
