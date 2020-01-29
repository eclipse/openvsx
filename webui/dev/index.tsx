import * as ReactDOM from 'react-dom';
import * as React from 'react';
import { Main } from '../src/main';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/styles';
import { createMuiTheme } from '@material-ui/core';

const theme = createMuiTheme({
    palette: {
        primary: { main: '#EEEEEE', contrastText: '#263238' },
        secondary: { main: '#42A5F5' }
    }
});

let serverHost = window.location.hostname;
if (serverHost.startsWith('3000-')) {
    // The frontend runs on port 3000, but the server runs on port 8080
    serverHost = '8080-' + serverHost.substring(5);
}

const node = document.getElementById('main');
ReactDOM.render(<BrowserRouter>
    <ThemeProvider theme={theme}>
        <Main
            apiUrl={`${window.location.protocol}//${serverHost}/api`}
            listHeaderTitle='Extensions for VS Code Compatible Editors'
            logoURL='/open-source.png'
            pageTitle='Open VSX Registry'
        />
    </ThemeProvider>
</BrowserRouter>, node);
