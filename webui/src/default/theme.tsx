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
            passed: {
                dark: '#2e5c32',
                light: '#a5d6a7',
            },
            quarantined: {
                dark: '#8e5518',
                light: '#ffcc80',
            },
            rejected: {
                dark: '#7d2e2e',
                light: '#ef9a9a',
            },
            errorStatus: {
                dark: '#5a5a5a',
                light: '#b0b0b0',
            },
            allowed: '#4caf50',
            blocked: '#f44336',
            review: '#ffc107',
            selected: {
                border: '#c160ef',
                background: '#3d1b4d',
                backgroundHover: '#4d2360',
                hover: 'rgba(255, 255, 255, 0.1)',
            },
            scanBackground: {
                default: '#1e1e1e',
                light: '#2d2d2d',
                dark: '#0a0a0a',
            },
            gray: {
                start: '#888888',
                middle: '#cccccc',
                end: '#888888',
                gradient: 'linear-gradient(90deg, #888888 0%, #cccccc 50%, #888888 100%)',
            },
            unenforced: {
                stripe: 'repeating-linear-gradient(-45deg, transparent, transparent 4px, rgba(255, 255, 255, 0.1) 4px, rgba(255, 255, 255, 0.1) 8px)',
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
