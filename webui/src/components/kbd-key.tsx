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

import { FunctionComponent, PropsWithChildren } from 'react';
import { Box } from '@mui/material';
import { MONO_FONT } from '../default/theme';

export const KbdKey: FunctionComponent<PropsWithChildren> = ({ children }) => (
    <Box
        component='kbd'
        sx={{
            fontFamily: MONO_FONT,
            fontSize: '0.625rem',
            fontWeight: 600,
            lineHeight: 1,
            // Bottom-heavy padding offsets the edge and lip below so the glyph sits
            // centered on the face; the min width squares single-letter caps.
            px: '0.3125rem',
            pt: '0.125rem',
            pb: '0.1875rem',
            minWidth: '1.125rem',
            textAlign: 'center',
            borderRadius: '4px',
            // All colors mix from the inherited text color, so the chip tints with
            // its context instead of being a fixed gray.
            color: 'color-mix(in srgb, currentcolor 62%, transparent)',
            bgcolor: 'color-mix(in srgb, currentcolor 9%, transparent)',
            border: '1px solid color-mix(in srgb, currentcolor 24%, transparent)',
            // Keycap depth: a thicker bottom edge plus a faint inner lip above it.
            borderBottomWidth: '2px',
            boxShadow: 'inset 0 -1px 0 color-mix(in srgb, currentcolor 12%, transparent)',
            display: 'inline-block',
            userSelect: 'none',
            verticalAlign: 'middle',
            // Pointless without a physical keyboard; the shortcuts modal re-shows its keys.
            '@media (hover: none)': { display: 'none' }
        }}>
        {children}
    </Box>
);
