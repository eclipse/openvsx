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
import { Box } from '@mui/material';

/** Width of the sidebar media (avatar/logo); the column is sized to it so it sits flush right. */
export const MEDIA_SIDEBAR_WIDTH = '10.75rem';

/**
 * GitHub-style settings layout: main content on the left, a media sidebar
 * (profile picture, namespace logo) flush with the right edge. On small
 * screens the media moves above the content, left aligned under the heading.
 */
export const MediaSidebarLayout: FunctionComponent<MediaSidebarLayoutProps> = ({ sidebar, children }) => (
    <Box
        sx={{
            display: 'grid',
            // Splits at lg, not md: with the settings sidebar already taking a column,
            // a second split below ~1040px squeezes the content to nothing.
            gridTemplateColumns: { xs: '1fr', lg: `1fr ${MEDIA_SIDEBAR_WIDTH}` },
            gap: { xs: '1.5rem', lg: '2.5rem' },
            alignItems: 'start'
        }}>
        <Box sx={{ minWidth: 0, order: { xs: 2, lg: 1 } }}>{children}</Box>
        <Box sx={{ width: MEDIA_SIDEBAR_WIDTH, order: { xs: 1, lg: 2 } }}>{sidebar}</Box>
    </Box>
);

export interface MediaSidebarLayoutProps {
    sidebar: ReactNode;
    children: ReactNode;
}
