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
import { Tab, Tabs, TabsProps } from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import { NAVBAR_HEIGHT } from '../default/theme';
import { accentHover, focusOutline, pillSurface } from './page-primitives';

// Category-pill look for the sticky tabs, floating over the nav bar's blur fan;
// the translucent fill matches the nav search field's treatment.
// eslint-disable-next-line react-refresh/only-export-components
export const PillTab = styled(Tab)(({ theme }) => ({
    ...pillSurface(theme),
    minHeight: 0,
    minWidth: 0,
    backgroundColor: alpha(theme.palette.surface2, 0.7),
    backdropFilter: 'blur(2px) saturate(1.8)',
    // Still translucent so the blur fan shows through; the border carries the emphasis.
    '&.Mui-selected': {
        backgroundColor: alpha(theme.palette.secondary.main, 0.7),
        borderColor: theme.palette.secondary.main,
        color: theme.palette.secondary.contrastText,
        fontWeight: 600
    },
    '&:not(.Mui-selected)': accentHover(theme),
    ...focusOutline(theme)
})) as typeof Tab;

export interface PillTabsProps extends TabsProps {
    /**
     * Page gutters per breakpoint. The strip bleeds through them so overflowing
     * pills scroll to the screen edge, and pads its scroller by the same amount
     * so the pills still line up with the content column.
     */
    gutters?: Record<string, string>;
}

/** Negative counterpart of each gutter, used to bleed the strip through them. */
const negateGutters = (gutters: Record<string, string>): Record<string, string> =>
    Object.fromEntries(Object.entries(gutters).map(([breakpoint, size]) => [breakpoint, `-${size}`]));

/**
 * Sticky, horizontally scrollable row of {@link PillTab}s pinned under the
 * navbar. The scroller keeps the selected pill in view when it changes.
 */
export const PillTabs: FunctionComponent<PillTabsProps> = ({
    gutters = { xs: '1rem', sm: '1.5rem' },
    sx,
    children,
    ...tabsProps
}) => {
    const bleed = negateGutters(gutters);
    return (
        <Tabs
            variant='scrollable'
            scrollButtons={false}
            sx={[
                {
                    // Pin under the navbar; the transparent row lets the blur fan
                    // back the pills (same z as the AppBar, later in the DOM).
                    position: 'sticky',
                    top: NAVBAR_HEIGHT,
                    zIndex: 50,
                    minHeight: 0,
                    mx: bleed,
                    '& .MuiTabs-indicator': { display: 'none' },
                    '& .MuiTabs-flexContainer': {
                        gap: '0.5rem',
                        // Inside the scroller — its overflow clips the focus ring otherwise.
                        py: '0.625rem',
                        // Sized to the pills so the trailing padding lands after the
                        // last pill, not at the 100% mark.
                        width: 'max-content',
                        minWidth: '100%',
                        px: gutters
                    }
                },
                ...(Array.isArray(sx) ? sx : [sx])
            ]}
            {...tabsProps}>
            {children}
        </Tabs>
    );
};
