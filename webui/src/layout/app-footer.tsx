/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent, useContext, useEffect, useState } from 'react';
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { KbdKey } from '../components/kbd-key';
import { OpenVsxMark } from '../components/openvsx-mark';
import { MONO_FONT } from '../default/theme';
import { MainContext } from '../context';

const FooterLink = styled('a')(({ theme }) => ({
    fontSize: '13.5px',
    color: theme.palette.text.secondary,
    textDecoration: 'none',
    display: 'block',
    '&:hover': { color: theme.palette.secondary.light }
}));

const FooterColumnHead = styled(Typography)(({ theme }) => ({
    fontSize: '12px',
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.08em',
    color: theme.palette.text.disabled,
    marginBottom: '14px'
}));

const SocialIconButton = styled('a')(({ theme }) => ({
    width: 34,
    height: 34,
    borderRadius: theme.shape.borderRadius,
    border: `1px solid ${theme.palette.divider}`,
    backgroundColor: theme.palette.background.paper,
    color: theme.palette.text.secondary,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    textDecoration: 'none',
    transition: 'border-color 0.14s, color 0.14s',
    '&:hover': {
        borderColor: theme.palette.secondary.main,
        color: theme.palette.secondary.light
    }
}));

const GitHubSvg = () => (
    <svg width='16' height='16' viewBox='0 0 24 24' fill='currentColor'>
        <path d='M12 1.5A10.5 10.5 0 001.5 12c0 4.64 3.01 8.57 7.18 9.96.53.1.72-.23.72-.5l-.01-1.76c-2.92.63-3.54-1.41-3.54-1.41-.48-1.21-1.17-1.54-1.17-1.54-.95-.65.07-.64.07-.64 1.06.07 1.61 1.09 1.61 1.09.94 1.6 2.46 1.14 3.06.87.1-.68.37-1.14.67-1.4-2.33-.27-4.78-1.17-4.78-5.2 0-1.15.41-2.09 1.08-2.82-.11-.27-.47-1.34.1-2.8 0 0 .88-.28 2.88 1.08a9.96 9.96 0 015.24 0c2-1.36 2.88-1.08 2.88-1.08.57 1.46.21 2.53.1 2.8.67.73 1.08 1.67 1.08 2.82 0 4.04-2.46 4.93-4.8 5.19.38.33.71.97.71 1.96l-.01 2.9c0 .28.19.61.73.5A10.5 10.5 0 0022.5 12 10.5 10.5 0 0012 1.5z' />
    </svg>
);

const LinkedInSvg = () => (
    <svg width='15' height='15' viewBox='0 0 24 24' fill='currentColor'>
        <path d='M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z' />
    </svg>
);

const XSvg = () => (
    <svg width='14' height='14' viewBox='0 0 24 24' fill='currentColor'>
        <path d='M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-4.714-6.231-5.401 6.231H2.744l7.737-8.857L1.254 2.25H8.08l4.259 5.631 5.905-5.631zm-1.161 17.52h1.833L7.084 4.126H5.117z' />
    </svg>
);

const FOOTER_LINKS = {
    resources: [
        { label: 'Documentation', href: 'https://github.com/eclipse/openvsx/wiki' },
        { label: 'API Reference', href: '/swagger-ui.html' },
        { label: 'Publishing Guide', href: 'https://github.com/eclipse/openvsx/wiki/Publishing-Extensions' },
        { label: 'Status', href: 'https://status.eclipse.org/' },
        { label: 'Commercial Usage', href: 'https://www.eclipse.org/legal/open-vsx-registry.php' }
    ],
    community: [
        { label: 'GitHub', href: 'https://github.com/eclipse/openvsx', external: true },
        { label: 'Working Group', href: 'https://openvsxworkinggroup.github.io/', external: true },
        { label: 'Report a Vulnerability', href: 'https://github.com/eclipse/openvsx/security', external: true },
        {
            label: 'Slack Workspace',
            href: 'https://join.slack.com/t/openvsxworkinggroup/shared_invite/zt-2y07y1ggy-ct3IfJljjGI6xWUQ9llv6A',
            external: true
        }
    ],
    legal: [
        { label: 'Privacy Policy', href: 'https://www.eclipse.org/legal/privacy.php', external: true },
        { label: 'Terms of Use', href: 'https://www.eclipse.org/legal/termsofuse.php', external: true },
        { label: 'Security Policy', href: 'https://github.com/eclipse/openvsx/security/policy', external: true },
        { label: 'Eclipse Foundation', href: 'https://www.eclipse.org', external: true }
    ]
};

const SOCIAL_LINKS = [
    { href: 'https://github.com/eclipse/openvsx', icon: <GitHubSvg />, title: 'GitHub' },
    { href: 'https://www.linkedin.com/company/eclipse-foundation/', icon: <LinkedInSvg />, title: 'LinkedIn' },
    { href: 'https://twitter.com/EclipseFdn', icon: <XSvg />, title: 'X (Twitter)' }
];

export interface AppFooterProps {
    onOpenShortcuts: () => void;
}

export const AppFooter: FunctionComponent<AppFooterProps> = ({ onOpenShortcuts }) => {
    const { service } = useContext(MainContext);
    const [version, setVersion] = useState<string | null>(null);

    useEffect(() => {
        const ac = new AbortController();
        service
            .getRegistryVersion(ac)
            .then(r => setVersion(r.version))
            .catch(() => {});
        return () => ac.abort();
    }, []);

    return (
        <Box
            component='footer'
            sx={{
                mt: 'auto',
                borderTop: '1px solid',
                borderColor: 'divider',
                bgcolor: 'background.paper'
            }}>
            {/* Columns */}
            <Box
                sx={{
                    maxWidth: '1320px',
                    mx: 'auto',
                    px: '28px',
                    pt: '48px',
                    pb: '30px',
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1.4fr 1fr 1fr 1fr' },
                    gap: '34px'
                }}>
                {/* Brand */}
                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: '9px', mb: '18px' }}>
                        <OpenVsxMark />
                        <Typography sx={{ fontWeight: 700, fontSize: '15px' }}>Open VSX Registry</Typography>
                    </Box>
                    <Typography
                        sx={{
                            fontSize: '13px',
                            color: 'text.disabled',
                            lineHeight: 1.55,
                            mb: '18px',
                            maxWidth: '260px'
                        }}>
                        An open-source, vendor-neutral registry for VS Code–compatible extensions.
                    </Typography>
                    <Box sx={{ display: 'flex', gap: '8px' }}>
                        {SOCIAL_LINKS.map(s => (
                            <SocialIconButton key={s.title} href={s.href} target='_blank' title={s.title}>
                                {s.icon}
                            </SocialIconButton>
                        ))}
                    </Box>
                </Box>
                {/* Resources */}
                <Box>
                    <FooterColumnHead>Resources</FooterColumnHead>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {FOOTER_LINKS.resources.map(l => (
                            <FooterLink key={l.label} href={l.href}>
                                {l.label}
                            </FooterLink>
                        ))}
                    </Box>
                </Box>
                {/* Community */}
                <Box>
                    <FooterColumnHead>Community</FooterColumnHead>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {FOOTER_LINKS.community.map(l => (
                            <FooterLink key={l.label} href={l.href} target='_blank'>
                                {l.label}
                            </FooterLink>
                        ))}
                        <Box
                            component='button'
                            onClick={onOpenShortcuts}
                            sx={{
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                                textAlign: 'left',
                                p: 0,
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                fontSize: '13.5px',
                                color: 'text.secondary',
                                '&:hover': { color: 'secondary.light' }
                            }}>
                            Keyboard shortcuts
                            <KbdKey>?</KbdKey>
                        </Box>
                    </Box>
                </Box>
                {/* Legal */}
                <Box>
                    <FooterColumnHead>Legal</FooterColumnHead>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {FOOTER_LINKS.legal.map(l => (
                            <FooterLink key={l.label} href={l.href} target='_blank'>
                                {l.label}
                            </FooterLink>
                        ))}
                    </Box>
                </Box>
            </Box>
            {/* Bottom bar */}
            <Box
                sx={{
                    maxWidth: '1320px',
                    mx: 'auto',
                    px: '28px',
                    py: '20px',
                    borderTop: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: '12px'
                }}>
                <Typography sx={{ fontSize: '12.5px', color: 'text.disabled' }}>
                    Copyright © Eclipse Foundation, AISBL. All Rights Reserved.
                </Typography>
                {version && (
                    <Typography sx={{ fontSize: '12.5px', color: 'text.disabled', fontFamily: MONO_FONT }}>
                        v{version}
                    </Typography>
                )}
            </Box>
        </Box>
    );
};
