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

import { FunctionComponent, ReactNode } from 'react';
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import type { Theme } from '@mui/material/styles';
import { Link as RouteLink } from 'react-router-dom';
import { HomeInvolvementCard } from '../../page-settings';
import { Section, Eyebrow, focusOutline } from '../../components/page-primitives';

const GetInvolvedCard = styled(Box)(({ theme }) => ({
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '16px',
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column'
}));

const getInvolvedLinkStyles = ({ theme }: { theme: Theme }) => ({
    fontSize: '0.8125rem',
    fontWeight: 600,
    color: theme.palette.secondary.light,
    textDecoration: 'none',
    '&:hover': { textDecoration: 'underline' },
    ...focusOutline(theme)
});

const GetInvolvedAnchorLink = styled('a')(getInvolvedLinkStyles);
const GetInvolvedRouteLink = styled(RouteLink)(getInvolvedLinkStyles);

interface GetInvolvedLinkProps {
    href: string;
    children: ReactNode;
}

const GetInvolvedLink: FunctionComponent<GetInvolvedLinkProps> = ({ href, children }) => {
    const external = /^https?:\/\//.test(href);
    const internalRoute = href.startsWith('/') && !href.startsWith('//');

    if (internalRoute) {
        return <GetInvolvedRouteLink to={href}>{children}</GetInvolvedRouteLink>;
    }

    return (
        <GetInvolvedAnchorLink
            href={href}
            target={external ? '_blank' : undefined}
            rel={external ? 'noopener noreferrer' : undefined}>
            {children}
        </GetInvolvedAnchorLink>
    );
};

export interface GetInvolvedProps {
    heading?: string;
    cards?: HomeInvolvementCard[];
}

/** Consumer-configured "Get Involved" cards (contribute, sponsor, etc.). */
export const GetInvolved: FunctionComponent<GetInvolvedProps> = ({ heading, cards }) => {
    if (!cards || cards.length === 0) {
        return null;
    }
    return (
        <Section component='section' sx={{ mt: { xs: '3rem', sm: '4.5rem' } }}>
            <Eyebrow sx={{ letterSpacing: '0.1em', mb: { xs: '0.875rem', sm: '1.25rem' } }}>
                {heading ?? 'Get Involved'}
            </Eyebrow>
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                    gap: '1rem'
                }}>
                {cards.map(card => {
                    return (
                        <GetInvolvedCard key={card.title}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: '0.6875rem', mb: '0.75rem' }}>
                                <Box
                                    sx={{
                                        width: '2.125rem',
                                        height: '2.125rem',
                                        borderRadius: '9px',
                                        bgcolor: 'accentSoft',
                                        color: 'secondary.light',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        flexShrink: 0,
                                        '& > svg': { fontSize: '1.125rem' }
                                    }}>
                                    {card.icon}
                                </Box>
                                <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700 }}>{card.title}</Typography>
                            </Box>
                            <Typography
                                sx={{
                                    fontSize: '0.8125rem',
                                    color: 'text.secondary',
                                    lineHeight: 1.55,
                                    mb: '1.125rem',
                                    flex: 1
                                }}>
                                {card.description}
                            </Typography>
                            <GetInvolvedLink href={card.href}>{card.label}</GetInvolvedLink>
                        </GetInvolvedCard>
                    );
                })}
            </Box>
        </Section>
    );
};
