/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { HomeSettings } from '../../page-settings';
import { Section, Eyebrow } from '../../components/layout';

const GetInvolvedCard = styled(Box)(({ theme }) => ({
    backgroundColor: theme.palette.background.paper,
    border: `1px solid ${theme.palette.divider}`,
    borderRadius: '16px',
    padding: '24px',
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
        <Section component='section' sx={{ mt: { xs: '48px', sm: '72px' }, mb: { xs: '40px', sm: '56px' } }}>
            <Eyebrow sx={{ letterSpacing: '0.1em', mb: { xs: '14px', sm: '20px' } }}>
                {involvement.heading ?? 'Get Involved'}
            </Eyebrow>
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                    gap: '16px'
                }}>
                {involvement.cards.map(card => (
                    <GetInvolvedCard key={card.title}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: '11px', mb: '12px' }}>
                            <Box
                                sx={{
                                    width: '34px',
                                    height: '34px',
                                    borderRadius: '9px',
                                    bgcolor: 'accentSoft',
                                    color: 'secondary.light',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    flexShrink: 0,
                                    '& > svg': { fontSize: 18 }
                                }}>
                                {card.icon}
                            </Box>
                            <Typography sx={{ fontSize: '15.5px', fontWeight: 700 }}>{card.title}</Typography>
                        </Box>
                        <Typography
                            sx={{
                                fontSize: '13.5px',
                                color: 'text.secondary',
                                lineHeight: 1.55,
                                mb: '18px',
                                flex: 1
                            }}>
                            {card.description}
                        </Typography>
                        <Box
                            component='a'
                            href={card.href}
                            target='_blank'
                            sx={{
                                fontSize: '13.5px',
                                fontWeight: 600,
                                color: 'secondary.light',
                                textDecoration: 'none',
                                '&:hover': { textDecoration: 'underline' }
                            }}>
                            {card.label}
                        </Box>
                    </GetInvolvedCard>
                ))}
            </Box>
        </Section>
    );
};
