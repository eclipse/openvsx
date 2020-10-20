/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createMuiTheme, Theme } from "@material-ui/core";

declare module '@material-ui/core/styles/createPalette' {
    interface Palette {
        neutral: {
            light: React.CSSProperties['color'],
            dark: React.CSSProperties['color']
        }
      }
    interface PaletteOptions {
        neutral: {
            light: React.CSSProperties['color'],
            dark: React.CSSProperties['color']
        }
    }
}
export default function createDefaultTheme(themeType: 'light' | 'dark'): Theme {
    return createMuiTheme({
        palette: {
            primary: {
                main: themeType === 'dark' ? '#444' : '#eeeeee',
                dark: themeType === 'dark' ? '#f4f4f4' : '#565157'
            },
            secondary: {
                main: themeType === 'dark' ? '#c160ef' : '#a60ee5',
                contrastText: '#edf5ea'
            },
            neutral: {
                light: themeType === 'dark' ? '#000' : '#e6e6e6',
                dark: themeType === 'dark' ? '#333' : '#fff',
            },
            type: themeType
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
