/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode } from 'react';
import { Helmet } from 'react-helmet-async';
import { Typography, Box } from '@mui/material';
import { Link as RouteLink, Route, useParams } from 'react-router-dom';
import GitHubIcon from '@mui/icons-material/GitHub';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import XIcon from '@mui/icons-material/X';
import CallSplitIcon from '@mui/icons-material/CallSplit';
import GroupsIcon from '@mui/icons-material/Groups';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { Extension, NamespaceDetails } from '../extension-registry-types';
import { PageSettings } from '../page-settings';
import { ExtensionListRoutes } from '../pages/extension-list/extension-list-routes';
import { DefaultMenuContent, MobileMenuContent } from './menu-content';
import { OpenVsxMark } from '../components/openvsx-mark';
import OpenVSXLogo from './openvsx-registry-logo';
import About from './about';
import { createAbsoluteURL } from '../utils';

const WIKI_URL = 'https://github.com/eclipse/openvsx/wiki';
const REPO_URL = 'https://github.com/eclipse/openvsx';
const SLACK_URL = 'https://join.slack.com/t/openvsxworkinggroup/shared_invite/zt-2y07y1ggy-ct3IfJljjGI6xWUQ9llv6A';

export default function createPageSettings(prefersDarkMode: boolean, serverUrl: string): PageSettings {
    const toolbarContent: FunctionComponent = () => (
        <RouteLink to={ExtensionListRoutes.MAIN} aria-label={`Home - Open VSX Registry`}>
            <OpenVSXLogo width='auto' height='40px' marginTop='8px' prefersDarkMode={prefersDarkMode} />
        </RouteLink>
    );

    const footer: PageSettings['elements']['footer'] = {
        brand: {
            logo: <OpenVsxMark />,
            name: 'Open VSX Registry',
            description: 'An open-source, vendor-neutral registry for VS Code–compatible extensions.'
        },
        columns: [
            {
                heading: 'Resources',
                links: [
                    { label: 'Documentation', href: WIKI_URL },
                    { label: 'API Reference', href: '/swagger-ui.html' },
                    { label: 'Publishing Guide', href: `${WIKI_URL}/Publishing-Extensions` },
                    { label: 'Status', href: 'https://status.eclipse.org/' },
                    { label: 'Commercial Usage', href: 'https://www.eclipse.org/legal/open-vsx-registry.php' }
                ]
            },
            {
                heading: 'Community',
                links: [
                    { label: 'GitHub', href: REPO_URL, external: true },
                    { label: 'Working Group', href: 'https://openvsxworkinggroup.github.io/', external: true },
                    { label: 'Report a Vulnerability', href: `${REPO_URL}/security`, external: true },
                    { label: 'Slack Workspace', href: SLACK_URL, external: true }
                ]
            },
            {
                heading: 'Legal',
                links: [
                    { label: 'Privacy Policy', href: 'https://www.eclipse.org/legal/privacy.php', external: true },
                    { label: 'Terms of Use', href: 'https://www.eclipse.org/legal/termsofuse.php', external: true },
                    { label: 'Security Policy', href: `${REPO_URL}/security/policy`, external: true },
                    { label: 'Eclipse Foundation', href: 'https://www.eclipse.org', external: true }
                ]
            }
        ],
        social: [
            { title: 'GitHub', href: REPO_URL, icon: <GitHubIcon sx={{ fontSize: 16 }} /> },
            {
                title: 'LinkedIn',
                href: 'https://www.linkedin.com/company/eclipse-foundation/',
                icon: <LinkedInIcon sx={{ fontSize: 16 }} />
            },
            { title: 'X (Twitter)', href: 'https://twitter.com/EclipseFdn', icon: <XIcon sx={{ fontSize: 15 }} /> }
        ],
        copyright: 'Copyright © Eclipse Foundation, AISBL. All Rights Reserved.'
    };

    const home: PageSettings['elements']['home'] = {
        popularSearches: ['python', 'git', 'docker', 'prettier', 'eslint', 'rust', 'java'],
        involvement: {
            heading: 'Get Involved',
            cards: [
                {
                    icon: <CallSplitIcon />,
                    title: 'Contribute',
                    description: 'Open VSX is fully open source. Help build the registry the ecosystem depends on.',
                    href: REPO_URL,
                    label: 'View on GitHub →'
                },
                {
                    icon: <GroupsIcon />,
                    title: 'Join the Working Group',
                    description: 'Shape the future of an open, vendor-neutral marketplace for extensions.',
                    href: 'https://openvsxworkinggroup.github.io/',
                    label: 'Learn more →'
                },
                {
                    icon: <MenuBookIcon />,
                    title: 'Read the docs',
                    description: 'Learn how to publish, claim namespaces, and consume extensions via the API.',
                    href: WIKI_URL,
                    label: 'Open documentation →'
                }
            ]
        }
    };

    const searchHeader: FunctionComponent = () => (
        <Box textAlign='center' sx={{ mb: 3, maxWidth: '700px', mx: 'auto' }}>
            <Box
                sx={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '8px',
                    px: '13px',
                    py: '6px',
                    borderRadius: '999px',
                    bgcolor: 'accentSoft',
                    color: 'secondary.light',
                    fontSize: '12.5px',
                    fontWeight: 600,
                    mb: 3
                }}>
                <Box
                    component='span'
                    sx={{
                        width: 7,
                        height: 7,
                        borderRadius: '50%',
                        bgcolor: 'secondary.main',
                        display: 'inline-block',
                        flexShrink: 0
                    }}
                />
                Open-source registry for VS Code–compatible editors
            </Box>
            <Typography
                component='h1'
                sx={{
                    fontSize: { xs: '2.2rem', sm: '3rem', md: '3.375rem' },
                    lineHeight: 1.04,
                    letterSpacing: '-0.035em',
                    fontWeight: 800,
                    mb: 2
                }}>
                Find the right extension,
                <br />
                for any editor.
            </Typography>
            <Typography
                sx={{ fontSize: '18px', color: 'text.secondary', maxWidth: '560px', mx: 'auto', lineHeight: 1.5 }}>
                Browse community-published extensions. <br />
                Free, open, and vendor-neutral.
            </Typography>
        </Box>
    );

    const additionalRoutes: ReactNode = <Route path='/about' element={<About />} />;

    const headTags: FunctionComponent<{ title: string }> = props => {
        return (
            <Helmet>
                <title>{props.title}</title>
            </Helmet>
        );
    };

    const mainHeadTags: FunctionComponent<{ pageSettings: PageSettings }> = props => {
        return headTags({ title: props.pageSettings.pageTitle });
    };

    const extensionHeadTags: FunctionComponent<{ extension?: Extension; pageSettings: PageSettings }> = props => {
        const params = useParams();
        const name = props.extension ? (props.extension.displayName ?? props.extension.name) : params.name;

        return headTags({ title: `${name} – ${props.pageSettings.pageTitle}` });
    };

    const namespaceHeadTags: FunctionComponent<{
        namespaceDetails?: NamespaceDetails;
        name: string;
        pageSettings: PageSettings;
    }> = props => {
        const name = props.namespaceDetails
            ? (props.namespaceDetails.displayName ?? props.namespaceDetails.name)
            : props.name;

        return headTags({ title: `${name} – ${props.pageSettings.pageTitle}` });
    };

    return {
        pageTitle: 'Open VSX Registry',
        themeType: prefersDarkMode ? 'dark' : 'light',
        publisherAgreement: {
            name: 'Open VSX'
        },
        elements: {
            toolbarContent,
            defaultMenuContent: DefaultMenuContent,
            mobileMenuContent: MobileMenuContent,
            footer,
            home,
            searchHeader,
            additionalRoutes,
            mainHeadTags,
            extensionHeadTags,
            namespaceHeadTags
        },
        urls: {
            extensionDefaultIcon: '/default-icon.png',
            namespaceAccessInfo: 'https://github.com/eclipse/openvsx/wiki/Namespace-Access',
            publisherAgreement: createAbsoluteURL([serverUrl, 'documents', 'publisher-agreement.md'])
        }
    };
}
