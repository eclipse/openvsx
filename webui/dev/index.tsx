import * as ReactDOM from 'react-dom';
import * as React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/styles';
import { createMuiTheme } from '@material-ui/core';
import { Main } from '../src/main';
import { ExtensionRegistryService } from '../src/extension-registry-service';

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
const service = new ExtensionRegistryService(`${window.location.protocol}//${serverHost}`);

const node = document.getElementById('main');
ReactDOM.render(<BrowserRouter>
    <ThemeProvider theme={theme}>
        <Main
            service={service}
            pageTitle='Open VSX Registry'
            toolbarText='Open VSX Registry'
            listHeaderTitle='Extensions for VS Code Compatible Editors'
            logoURL='/open-source.png'
            namespaceAccessInfoURL='https://github.com/eclipse/openvsx/wiki/Namespace-Access'
        />
    </ThemeProvider>
</BrowserRouter>, node);
