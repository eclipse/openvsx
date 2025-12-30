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
import { Main } from '../main';
import createPageSettings from './page-settings';
import createDefaultTheme from './theme';
import { store } from '../store/store';
import { Provider } from 'react-redux';

// This is the default entry point for the webui Docker image and for development.
// The production code for open-vsx.org is at https://github.com/eclipse/open-vsx.org

const App = () => {
    const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');
    const theme = useMemo(
        () => createDefaultTheme(prefersDarkMode ? 'dark' : 'light'),
        [prefersDarkMode],
    );

    const pageSettings = createPageSettings(prefersDarkMode);
    return (
        <Provider store={store}>
            <HelmetProvider>
                <ThemeProvider theme={theme}>
                    <Main
                        pageSettings={pageSettings}
                    />
                </ThemeProvider>
            </HelmetProvider>
        </Provider>
    );
};

const node = document.getElementById('main') as HTMLElement;
const root = createRoot(node);
root.render(<BrowserRouter>
    <App />
</BrowserRouter>);
