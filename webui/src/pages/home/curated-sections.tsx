/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { ExtensionCard } from '../../components/extension-card';
import { Section } from '../../components/layout';
import { CURATED_SIZE, CuratedRow } from './use-home-data';

interface CuratedSectionsProps {
    rows: CuratedRow[];
    onSeeAll: () => void;
}

/** Renders the curated extension rows (e.g. "Most downloaded"), skipping empty/loading ones. */
export const CuratedSections: FunctionComponent<CuratedSectionsProps> = ({ rows, onSeeAll }) => (
    <>
        {rows.map(
            row =>
                !row.loading &&
                row.extensions.length > 0 && (
                    <Section component='section' key={row.title} sx={{ mt: { xs: '36px', sm: '54px' } }}>
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                mb: '18px'
                            }}>
                            <Box>
                                <Typography
                                    sx={{
                                        fontSize: { xs: '16px', sm: '23px' },
                                        fontWeight: 700,
                                        letterSpacing: '-0.02em'
                                    }}>
                                    {row.title}
                                </Typography>
                                <Typography
                                    component='span'
                                    sx={{
                                        fontSize: '13.5px',
                                        color: 'text.disabled',
                                        display: { xs: 'none', sm: 'block' }
                                    }}>
                                    {row.subtitle}
                                </Typography>
                            </Box>
                            <Box
                                component='button'
                                onClick={onSeeAll}
                                sx={{
                                    background: 'none',
                                    border: 'none',
                                    color: 'secondary.light',
                                    fontSize: '14px',
                                    fontWeight: 600,
                                    cursor: 'pointer'
                                }}>
                                See all →
                            </Box>
                        </Box>
                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: {
                                    xs: 'repeat(2, 1fr)',
                                    sm: 'repeat(auto-fill, minmax(190px, 1fr))'
                                },
                                gap: '16px'
                            }}>
                            {row.extensions.map((ext, idx) => (
                                <ExtensionCard
                                    key={`${ext.namespace}.${ext.name}`}
                                    extension={ext}
                                    idx={idx}
                                    filterSize={CURATED_SIZE}
                                />
                            ))}
                        </Box>
                    </Section>
                )
        )}
    </>
);
