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
import type {} from '@mui/x-data-grid/themeAugmentation';

export const MONO_FONT = "'Geist Mono', monospace";
export const NAVBAR_HEIGHT = '3.875rem';
// Pixel twin for scroll math (rem values assume the 16px root font size).
export const NAVBAR_HEIGHT_PX = parseFloat(NAVBAR_HEIGHT) * 16;

// Shared look of floating surfaces (menus, popovers, dialogs). The nested selector
// outranks MuiPaper's own rounded style, so no !important is needed.
const floatingPaper = (theme: Theme) => ({
    border: `1px solid ${theme.palette.divider}`,
    boxShadow: 'var(--shadow-lg)',
    backgroundColor: theme.palette.background.paper,
    backgroundImage: 'none',
    '&.MuiPaper-rounded': { borderRadius: theme.shape.borderRadiusCard }
});

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
    // Surface tiers not covered by MUI's background.paper
    surface2: string;
    surface3: string;
    border2: string;
    accentSoft: string;
    warningSoft: string;
    warningAccent: string;
    bg2: string;
}

declare module '@mui/material/styles/createPalette' {
    // eslint-disable-next-line @typescript-eslint/no-empty-object-type
    interface Palette extends CustomPaletteColors {}
    interface PaletteOptions extends Partial<CustomPaletteColors> {
        neutral: NeutralColors;
        textHint: Color;
    }
}

declare module '@mui/system/createTheme/shape' {
    interface Shape {
        borderRadiusCard: number;
    }
}

export default function createDefaultTheme(themeType: 'light' | 'dark'): Theme {
    const dark = themeType === 'dark';
    return createTheme({
        typography: {
            fontFamily: "'Geist', 'Roboto', system-ui, -apple-system, sans-serif"
        },
        shape: {
            borderRadius: 9,
            borderRadiusCard: 14
        },
        mixins: {
            toolbar: { minHeight: NAVBAR_HEIGHT }
        },
        palette: {
            mode: themeType,
            // Standard MUI palette — these replace the matching CSS vars
            background: {
                default: dark ? '#0c0c11' : '#ffffff',
                paper: dark ? '#15151d' : '#ffffff'
            },
            text: {
                primary: dark ? '#ededf2' : '#16161c',
                secondary: dark ? '#b2b2bf' : '#54545f',
                disabled: dark ? '#7a7a87' : '#8c8c98'
            },
            divider: dark ? '#262630' : '#e9e9ee',
            primary: {
                main: dark ? '#ededf2' : '#16161c',
                dark: dark ? '#f4f4f4' : '#0d0d11'
            },
            secondary: {
                main: dark ? '#a855f7' : '#8b1fd6',
                dark: dark ? '#9333ea' : '#7916bd',
                light: dark ? '#c084fc' : '#8b1fd6', // accent-fg
                contrastText: '#ffffff'
            },
            // Custom surface tiers
            bg2: dark ? '#101016' : '#f7f7f9',
            surface2: dark ? '#1a1a23' : '#fafafa',
            surface3: dark ? '#20202b' : '#f2f2f5',
            border2: dark ? '#1d1d26' : '#f0f0f3',
            accentSoft: dark ? '#291a3d' : '#f4e9fd',
            warningSoft: dark ? '#3a2c14' : '#fdf3e2',
            warningAccent: dark ? '#fbbf24' : '#d97706',
            // Legacy admin palette
            neutral: {
                light: dark ? '#000' : '#e6e6e6',
                dark: dark ? '#151515' : '#fff'
            },
            textHint: 'rgba(0, 0, 0, 0.38)',
            checkboxUnchecked: dark ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
            passed: {
                dark: dark ? '#2e5c32' : '#4db052',
                light: dark ? '#a5d6a7' : '#c8e6c9'
            },
            quarantined: {
                dark: dark ? '#8e5518' : '#e09030',
                light: dark ? '#ffcc80' : '#ffe0b2'
            },
            rejected: {
                dark: dark ? '#7d2e2e' : '#d63c3c',
                light: dark ? '#ef9a9a' : '#ffcdd2'
            },
            errorStatus: {
                dark: dark ? '#5a5a5a' : '#8a8a8a',
                light: dark ? '#b0b0b0' : '#e0e0e0'
            },
            allowed: '#4caf50',
            blocked: '#f44336',
            review: '#e6a800',
            selected: {
                border: '#c160ef',
                background: dark ? '#3d1b4d' : '#f3e5f9',
                backgroundHover: dark ? '#4d2360' : '#e9d5f5',
                hover: dark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.04)'
            },
            scanBackground: {
                default: dark ? '#1e1e1e' : '#f5f5f5',
                light: dark ? '#2d2d2d' : '#f0f0f0',
                dark: dark ? '#0a0a0a' : '#fafafa'
            },
            gray: {
                start: '#888888',
                middle: '#cccccc',
                end: '#888888',
                gradient: 'linear-gradient(90deg, #888888 0%, #cccccc 50%, #888888 100%)'
            },
            unenforced: {
                stripe: dark
                    ? 'repeating-linear-gradient(-45deg, transparent, transparent 4px, rgba(255, 255, 255, 0.12) 4px, rgba(255, 255, 255, 0.12) 8px)'
                    : 'repeating-linear-gradient(-45deg, transparent, transparent 4px, rgba(0, 0, 0, 0.12) 4px, rgba(0, 0, 0, 0.12) 8px)'
            }
        },
        breakpoints: {
            values: { xs: 0, sm: 550, md: 800, lg: 1040, xl: 1240 }
        },
        components: {
            MuiAccordion: {
                styleOverrides: {
                    root: {
                        border: 0,
                        boxShadow: 'none',
                        background: 'transparent',
                        '&:before': { display: 'none' }
                    }
                }
            },
            MuiButton: {
                styleOverrides: {
                    root: { textTransform: 'none' }
                }
            },
            // MUI X derives the grid's borders from `divider` via lighten/darken, which
            // almost erases our already-light opaque divider. Feed the grid's design
            // tokens directly; `&.MuiDataGrid-root` outranks the injected
            // `.MuiDataGridVariables-*` class that carries the derived defaults.
            MuiDataGrid: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        '&.MuiDataGrid-root': {
                            '--DataGrid-t-color-border-base': theme.palette.divider,
                            '--DataGrid-t-header-background-base': theme.palette.bg2,
                            '--DataGrid-t-radius-base': `${theme.shape.borderRadiusCard}px`,
                            '--DataGrid-t-color-interactive-focus': theme.palette.secondary.main,
                            '--DataGrid-t-color-interactive-selected': theme.palette.secondary.main,
                            '--DataGrid-t-color-foreground-accent': theme.palette.secondary.light
                        }
                    }),
                    columnHeaderTitle: ({ theme }) => ({
                        fontSize: '0.8125rem',
                        fontWeight: 600,
                        color: theme.palette.text.secondary
                    })
                }
            },
            MuiTableCell: {
                styleOverrides: {
                    root: ({ theme }) => ({ borderBottomColor: theme.palette.divider })
                }
            },
            MuiTabs: {
                styleOverrides: {
                    indicator: ({ theme }) => ({
                        backgroundColor: theme.palette.secondary.main,
                        height: '2px'
                    })
                }
            },
            MuiTab: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        fontSize: '0.875rem',
                        fontWeight: 600,
                        textTransform: 'none',
                        color: theme.palette.text.disabled,
                        minHeight: '3.25rem',
                        padding: '0.9375rem 1rem',
                        '&.Mui-selected': { color: theme.palette.text.primary }
                    })
                }
            },
            // Menu and Select popups inherit the floating look from MuiPopover.
            MuiMenu: {
                styleOverrides: {
                    paper: { marginTop: '0.375rem' },
                    list: { padding: '0.375rem' }
                }
            },
            MuiMenuItem: {
                styleOverrides: {
                    root: ({ theme }) => ({
                        borderRadius: theme.shape.borderRadius,
                        fontSize: '0.875rem',
                        fontWeight: 500,
                        minHeight: '2.25rem'
                    })
                }
            },
            MuiTypography: {
                styleOverrides: {
                    button: {
                        textTransform: 'none',
                        fontWeight: 500,
                        letterSpacing: 0
                    },
                    overline: {
                        textTransform: 'none',
                        letterSpacing: 0,
                        lineHeight: 1.4
                    }
                }
            },
            MuiPopover: {
                styleOverrides: {
                    paper: ({ theme }) => floatingPaper(theme)
                }
            },
            MuiDialog: {
                styleOverrides: {
                    paper: ({ theme }) => floatingPaper(theme)
                }
            },
            MuiDivider: {
                styleOverrides: {
                    root: ({ theme }) => ({ borderColor: theme.palette.divider })
                }
            },
            MuiOutlinedInput: {
                styleOverrides: {
                    notchedOutline: ({ theme }) => ({
                        borderColor: theme.palette.divider
                    })
                }
            }
        }
    });
}
