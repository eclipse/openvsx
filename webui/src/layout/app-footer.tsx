/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useState } from 'react';
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import type { Theme } from '@mui/material/styles';
import { Link as RouteLink } from 'react-router-dom';
import { KbdKey } from '../components/kbd-key';
import { useShortcut } from '../hooks/use-shortcut';
import { Section, Eyebrow, accentHover, focusOutline } from '../components/page-primitives';
import { MONO_FONT } from '../default/theme';
import { CustomFooterSettings, StructuredFooterSettings } from '../page-settings';
import { MainContext } from '../context';

const footerLinkStyles = ({ theme }: { theme: Theme }) => ({
    fontSize: '0.8125rem',
    color: theme.palette.text.secondary,
    textDecoration: 'none',
    display: 'block',
    '&:hover': { color: theme.palette.secondary.light },
    ...focusOutline(theme)
});

const FooterLink = styled('a')(footerLinkStyles);
const FooterRouteLink = styled(RouteLink)(footerLinkStyles);

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
    ...accentHover(theme),
    ...focusOutline(theme)
}));

const ShortcutsButton = styled('button')(({ theme }) => ({
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: 0,
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    fontSize: '0.75rem',
    color: theme.palette.text.disabled,
    '&:hover': { color: theme.palette.secondary.light },
    ...focusOutline(theme)
}));

/** Legacy footer chrome: fixed to the viewport bottom as on pre-redesign layouts, hover toggles `expanded`. */
const LegacyFooterRoot = styled('footer')(({ theme }) => ({
    position: 'fixed',
    bottom: 0,
    zIndex: 50,
    width: '100%',
    padding: `${theme.spacing(1)} ${theme.spacing(2)}`,
    backgroundColor: theme.palette.background.paper,
    boxShadow: '0px -2px 6px 0px rgba(0, 0, 0, 0.5)',
    backgroundImage:
        theme.palette.mode === 'dark'
            ? 'linear-gradient(rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.08))'
            : undefined
}));

const LegacyFooter: FunctionComponent<{ footer: CustomFooterSettings }> = ({ footer }) => {
    const Content = footer.content;
    const [expanded, setExpanded] = useState(false);
    return (
        <LegacyFooterRoot onMouseEnter={() => setExpanded(true)} onMouseLeave={() => setExpanded(false)}>
            <Content expanded={expanded} />
        </LegacyFooterRoot>
    );
};

export interface AppFooterProps {
    onOpenShortcuts: () => void;
}

export const AppFooter: FunctionComponent<AppFooterProps> = ({ onOpenShortcuts }) => {
    const { pageSettings, version } = useContext(MainContext);
    const footer = pageSettings.elements.footer;

    // The shortcuts button (and its ? hint) only renders in the structured footer,
    // so register the shortcut here and disable it for custom/legacy footers.
    const isStructuredFooter = typeof footer !== 'function' && !(footer && 'content' in footer);
    useShortcut({
        key: '?',
        label: 'Show keyboard shortcuts',
        order: 0,
        callback: onOpenShortcuts,
        enabled: isStructuredFooter
    });

    if (typeof footer === 'function') {
        const CustomFooter = footer;
        return <CustomFooter />;
    }
    if (footer && 'content' in footer) {
        return <LegacyFooter footer={footer} />;
    }

    return <StructuredFooter footer={footer} version={version?.version ?? null} onOpenShortcuts={onOpenShortcuts} />;
};

interface StructuredFooterProps {
    footer?: StructuredFooterSettings;
    version: string | null;
    onOpenShortcuts: () => void;
}

const StructuredFooter: FunctionComponent<StructuredFooterProps> = ({ footer, version, onOpenShortcuts }) => (
    <Box
        component='footer'
        sx={{ mt: 'auto', borderTop: '1px solid', borderColor: 'divider', bgcolor: 'background.paper' }}>
        {footer && (footer.brand || footer.columns) && (
            <Section
                sx={{
                    pt: '3rem',
                    pb: '1.875rem',
                    display: 'grid',
                    // Brand cell takes the slack so the link columns stay narrow and flush right.
                    gridTemplateColumns: {
                        xs: '1fr',
                        sm: '1fr 1fr',
                        md: `1fr repeat(${footer?.columns?.length ?? 0}, minmax(0, 13rem))`
                    },
                    gap: '2.125rem'
                }}>
                {footer.brand && (
                    <Box>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: '0.5625rem', mb: '1.125rem' }}>
                            {footer.brand.logo}
                            <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem' }}>{footer.brand.name}</Typography>
                        </Box>
                        {footer.brand.description && (
                            <Typography
                                sx={{
                                    fontSize: '0.8125rem',
                                    color: 'text.disabled',
                                    lineHeight: 1.55,
                                    mb: '1.125rem',
                                    maxWidth: '16.25rem'
                                }}>
                                {footer.brand.description}
                            </Typography>
                        )}
                        {footer.social && footer.social.length > 0 && (
                            <Box sx={{ display: 'flex', gap: '0.5rem' }}>
                                {footer.social.map(s => (
                                    <SocialIconButton key={s.title} href={s.href} target='_blank' title={s.title}>
                                        {s.icon}
                                    </SocialIconButton>
                                ))}
                            </Box>
                        )}
                    </Box>
                )}
                {footer.columns?.map((column, ci) => (
                    <Box key={ci}>
                        <Eyebrow sx={{ mb: '0.875rem' }}>{column.heading}</Eyebrow>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
                            {column.links.map((l, li) =>
                                !l.external && l.href.startsWith('/') && !l.href.startsWith('//') ? (
                                    <FooterRouteLink key={li} to={l.href}>
                                        {l.label}
                                    </FooterRouteLink>
                                ) : (
                                    <FooterLink key={li} href={l.href} target={l.external ? '_blank' : undefined}>
                                        {l.label}
                                    </FooterLink>
                                )
                            )}
                        </Box>
                    </Box>
                ))}
            </Section>
        )}
        <Section
            sx={{
                py: '1.25rem',
                borderTop: footer?.brand || footer?.columns ? '1px solid' : 'none',
                borderColor: 'divider',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '0.75rem'
            }}>
            <Typography sx={{ fontSize: '0.75rem', color: 'text.disabled' }}>{footer?.copyright}</Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
                {footer?.extra}
                <ShortcutsButton type='button' onClick={onOpenShortcuts}>
                    Keyboard shortcuts
                    <KbdKey>?</KbdKey>
                </ShortcutsButton>
                {version && (
                    <Typography sx={{ fontSize: '0.75rem', color: 'text.disabled', fontFamily: MONO_FONT }}>
                        {version}
                    </Typography>
                )}
            </Box>
        </Section>
    </Box>
);
