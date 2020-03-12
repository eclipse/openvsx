import * as ReactDOM from 'react-dom';
import * as React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/styles';
import { createMuiTheme } from '@material-ui/core';
import { Main } from '../src/main';
import { ExtensionRegistryService } from '../src/extension-registry-service';
import { Extension } from '../src/extension-registry-types';
import { PageSettings } from '../src/page-settings';

const theme = createMuiTheme({
    palette: {
        primary: { main: '#EEEEEE', contrastText: '#263238' },
        secondary: { main: '#42A5F5' }
    }
});

let serverHost = location.hostname;
if (serverHost.startsWith('3000-')) {
    // The frontend runs on port 3000, but the server runs on port 8080
    serverHost = '8080-' + serverHost.substring(5);
}
const service = new ExtensionRegistryService(`${location.protocol}//${serverHost}`);

const reportAbuseText = encodeURIComponent('<Please describe the issue>');
const claimNamespaceText = encodeURIComponent('<Please explain why you want exclusive access to this namespace (e.g. related to a company, source repository owner, etc.)>');
const extensionURL = (extension: Extension) => encodeURIComponent(
    `${location.protocol}//${location.hostname}/extension/${extension.namespace}/${extension.name}`);
const pageSettings: PageSettings = {
    pageTitle: 'Open VSX Registry',
    toolbarText: 'Open VSX Registry',
    listHeaderTitle: 'Extensions for VS Code Compatible Editors',
    logoURL: '/open-source.png',
    namespaceAccessInfoURL: 'https://github.com/eclipse/openvsx/wiki/Namespace-Access',
    reportAbuseHref: extension => `mailto:abuse@example.com?subject=Report%20Abuse%20-%20${extension.namespace}.${extension.name}&Body=${reportAbuseText}%0A%0A${extensionURL(extension)}`,
    claimNamespaceHref: namespace => `mailto:claim@example.com?subject=Claim%20Namespace%20Ownership%20-%20${namespace}&Body=${claimNamespaceText}`
};

const node = document.getElementById('main');
ReactDOM.render(<BrowserRouter>
    <ThemeProvider theme={theme}>
        <Main
            service={service}
            pageSettings={pageSettings}
        />
    </ThemeProvider>
</BrowserRouter>, node);
