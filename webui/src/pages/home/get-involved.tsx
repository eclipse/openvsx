/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { HomeSettings } from '../../page-settings';
import { Section, Eyebrow, focusOutline } from '../../components/page-primitives';

const GetInvolvedCard = styled(Box)(({ theme }) => ({
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '16px',
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column'
}));

interface GetInvolvedProps {
    involvement: HomeSettings['involvement'];
}

/** Consumer-configured "Get Involved" cards (contribute, sponsor, etc.). */
export const GetInvolved: FunctionComponent<GetInvolvedProps> = ({ involvement }) => {
    if (!involvement || involvement.cards.length === 0) {
        return null;
    }
    return (
        <Section component='section' sx={{ mt: { xs: '3rem', sm: '4.5rem' }, mb: { xs: '2.5rem', sm: '3.5rem' } }}>
            <Eyebrow sx={{ letterSpacing: '0.1em', mb: { xs: '0.875rem', sm: '1.25rem' } }}>
                {involvement.heading ?? 'Get Involved'}
            </Eyebrow>
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                    gap: '1rem'
                }}>
                {involvement.cards.map(card => (
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
                        <Box
                            component='a'
                            href={card.href}
                            target='_blank'
                            sx={theme => ({
                                fontSize: '0.8125rem',
                                fontWeight: 600,
                                color: 'secondary.light',
                                textDecoration: 'none',
                                '&:hover': { textDecoration: 'underline' },
                                ...focusOutline(theme)
                            })}>
                            {card.label}
                        </Box>
                    </GetInvolvedCard>
                ))}
            </Box>
        </Section>
    );
};
