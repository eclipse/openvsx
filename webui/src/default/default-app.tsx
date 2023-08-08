/********************************************************************************
 * Copyright (c) 2019-2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { createRoot } from 'react-dom/client';
import React, { useMemo } from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { ExtensionRegistryService } from '../extension-registry-service';
import { Main } from '../main';
import createPageSettings from './page-settings';
import createDefaultTheme from './theme';

// This is the default entry point for the webui Docker image and for development.
// The production code for open-vsx.org is at https://github.com/eclipse/open-vsx.org


let serverHost = location.hostname;
if (serverHost.startsWith('3000-')) {
    // Gitpod dev environment: the frontend runs on port 3000, but the server runs on port 8080
    serverHost = '8080-' + serverHost.substring(5);
} else if (location.port === '3000') {
    // Localhost dev environment
    serverHost = serverHost + ':8080';
}
const service = new ExtensionRegistryService(`${location.protocol}//${serverHost}`);

const App = () => {
    const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');
    const theme = useMemo(
        () => createDefaultTheme(prefersDarkMode ? 'dark' : 'light'),
        [prefersDarkMode],
    );

    const pageSettings = createPageSettings(prefersDarkMode, service.serverUrl);
    return (
        <HelmetProvider>
            <ThemeProvider theme={theme}>
                <Main
                    service={service}
                    pageSettings={pageSettings}
                />
            </ThemeProvider>
        </HelmetProvider>
    );
};

const node = document.getElementById('main') as HTMLElement;
const root = createRoot(node);
root.render(<BrowserRouter>
    <App />
</BrowserRouter>);
