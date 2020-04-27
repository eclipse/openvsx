/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as ReactDOM from 'react-dom';
import * as React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/styles';
import { createMuiTheme } from '@material-ui/core';
import { Main } from '../src/main';
import { ExtensionRegistryService } from '../src/extension-registry-service';
import createPageSettings from './page-settings';

// This file is the main entry point for the development setup.
// The production code is at https://github.com/eclipse/open-vsx.org

const theme = createMuiTheme({
    palette: {
        primary: { main: '#eeeeee', contrastText: '#3f3841', dark: '#565157' },
        secondary: { main: '#a60ee5', contrastText: '#edf5ea' }
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

let serverHost = location.hostname;
if (serverHost.startsWith('3000-')) {
    // The frontend runs on port 3000, but the server runs on port 8080
    serverHost = '8080-' + serverHost.substring(5);
}
const service = new ExtensionRegistryService(`${location.protocol}//${serverHost}`);

const pageSettings = createPageSettings(theme);

const node = document.getElementById('main');
ReactDOM.render(<BrowserRouter>
    <ThemeProvider theme={theme}>
        <Main
            service={service}
            pageSettings={pageSettings}
        />
    </ThemeProvider>
</BrowserRouter>, node);
