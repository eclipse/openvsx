/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { CSSProperties } from 'react';
import { createTheme, Theme } from '@mui/material';

// Shared type definitions for palette extensions
type Color = CSSProperties['color'];

interface StatusColors {
    dark: Color;
    light: Color;
}

interface NeutralColors {
    light: Color;
    dark: Color;
}

interface SelectedColors {
    border: Color;
    background: Color;
    backgroundHover: Color;
    hover: Color;
}

interface ScanBackgroundColors {
    default: Color;
    light: Color;
    dark: Color;
}

interface GrayColors {
    start: Color;
    middle: Color;
    end: Color;
    gradient: string;
}

interface UnenforcedColors {
    stripe: string;
}

// Shared shape for custom palette properties
interface CustomPaletteColors {
    neutral: NeutralColors;
    textHint: Color;
    checkboxUnchecked: Color;
    passed: StatusColors;
    quarantined: StatusColors;
    rejected: StatusColors;
    errorStatus: StatusColors;
    allowed: Color;
    blocked: Color;
    review: Color;
    selected: SelectedColors;
    scanBackground: ScanBackgroundColors;
    gray: GrayColors;
    unenforced: UnenforcedColors;
}

declare module '@mui/material/styles/createPalette' {
    // eslint-disable-next-line @typescript-eslint/no-empty-object-type
    interface Palette extends CustomPaletteColors {}
    interface PaletteOptions extends Partial<CustomPaletteColors> {
        neutral: NeutralColors;
        textHint: Color;
    }
}
export default function createDefaultTheme(themeType: 'light' | 'dark'): Theme {
    return createTheme({
        palette: {
            primary: {
                main: themeType === 'dark' ? '#eeeeee' : '#444',
                dark: themeType === 'dark' ? '#f4f4f4' : '#565157'
            },
            secondary: {
                main: themeType === 'dark' ? '#c160ef' : '#a60ee5',
                contrastText: '#edf5ea'
            },
            neutral: {
                light: themeType === 'dark' ? '#000' : '#e6e6e6',
                dark: themeType === 'dark' ? '#151515' : '#fff',
            },
            textHint: 'rgba(0, 0, 0, 0.38)',
            checkboxUnchecked: themeType === 'dark' ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
            passed: {
                dark: themeType === 'dark' ? '#2e5c32' : '#4db052',
                light: themeType === 'dark' ? '#a5d6a7' : '#c8e6c9',
            },
            quarantined: {
                dark: themeType === 'dark' ? '#8e5518' : '#e09030',
                light: themeType === 'dark' ? '#ffcc80' : '#ffe0b2',
            },
            rejected: {
                dark: themeType === 'dark' ? '#7d2e2e' : '#d63c3c',
                light: themeType === 'dark' ? '#ef9a9a' : '#ffcdd2',
            },
            errorStatus: {
                dark: themeType === 'dark' ? '#5a5a5a' : '#8a8a8a',
                light: themeType === 'dark' ? '#b0b0b0' : '#e0e0e0',
            },
            allowed: '#4caf50',
            blocked: '#f44336',
            review: '#e6a800',
            selected: {
                border: '#c160ef',
                background: themeType === 'dark' ? '#3d1b4d' : '#f3e5f9',
                backgroundHover: themeType === 'dark' ? '#4d2360' : '#e9d5f5',
                hover: themeType === 'dark' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.04)',
            },
            scanBackground: {
                default: themeType === 'dark' ? '#1e1e1e' : '#f5f5f5',
                light: themeType === 'dark' ? '#2d2d2d' : '#f0f0f0',
                dark: themeType === 'dark' ? '#0a0a0a' : '#fafafa',
            },
            gray: {
                start: '#888888',
                middle: '#cccccc',
                end: '#888888',
                gradient: 'linear-gradient(90deg, #888888 0%, #cccccc 50%, #888888 100%)',
            },
            unenforced: {
                stripe: themeType === 'dark'
                    ? 'repeating-linear-gradient(-45deg, transparent, transparent 4px, rgba(255, 255, 255, 0.12) 4px, rgba(255, 255, 255, 0.12) 8px)'
                    : 'repeating-linear-gradient(-45deg, transparent, transparent 4px, rgba(0, 0, 0, 0.12) 4px, rgba(0, 0, 0, 0.12) 8px)',
            },
            mode: themeType
        },
        breakpoints: {
            values: {
                xs: 340,
                sm: 550,
                md: 800,
                lg: 1040,
                xl: 1240
            }
        }
    });
}
