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

import { FunctionComponent } from 'react';
import { Box, Typography } from '@mui/material';
import { ExtensionCard } from '../../components/extension-card';
import { Section } from '../../components/page-primitives';
import { HomeCuratedSection } from '../../page-settings';
import { useSearch } from '../../hooks/use-search';
import { CURATED_SIZE, DEFAULT_CURATED_SECTIONS, useCuratedRows } from './use-home-data';

export interface CuratedSectionsProps {
    /** Rows to fetch and render; defaults to "Most downloaded" and "Recently updated". */
    sections?: HomeCuratedSection[];
    /** "See all" click handler; defaults to opening the search page unfiltered. */
    onSeeAll?: () => void;
}

/** Renders the curated extension rows (e.g. "Most downloaded"), skipping empty/loading ones. */
export const CuratedSections: FunctionComponent<CuratedSectionsProps> = props => {
    const rows = useCuratedRows(props.sections ?? DEFAULT_CURATED_SECTIONS);
    const { search } = useSearch();
    return (
        <>
            {rows.map(row => {
                // Hide rows that loaded empty (e.g. a failed request).
                if (!row.loading && row.extensions.length === 0) {
                    return null;
                }
                return (
                    <Section component='section' key={row.title} sx={{ mt: { xs: '2.25rem', sm: '3.375rem' } }}>
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                mb: '1.125rem'
                            }}>
                            <Box>
                                <Typography
                                    sx={{
                                        fontSize: { xs: '1rem', sm: '1.4375rem' },
                                        fontWeight: 700,
                                        letterSpacing: '-0.02em'
                                    }}>
                                    {row.title}
                                </Typography>
                                <Typography
                                    component='span'
                                    sx={{
                                        fontSize: '0.8125rem',
                                        color: 'text.disabled',
                                        display: { xs: 'none', sm: 'block' }
                                    }}>
                                    {row.subtitle}
                                </Typography>
                            </Box>
                            <Box
                                component='button'
                                onClick={props.onSeeAll ?? (() => search({ query: '', sortBy: row.sortBy }))}
                                sx={{
                                    background: 'none',
                                    border: 'none',
                                    color: 'secondary.light',
                                    fontSize: '0.875rem',
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
                                    xs: 'repeat(2, minmax(0, 1fr))',
                                    sm: 'repeat(auto-fill, minmax(190px, 1fr))'
                                },
                                gap: '1rem'
                            }}>
                            {/* Fixed index-keyed slots so cards stay mounted across the swap and don't re-fade. */}
                            {Array.from({ length: row.loading ? CURATED_SIZE : row.extensions.length }, (_, idx) => (
                                <ExtensionCard
                                    key={idx}
                                    extension={row.extensions[idx]}
                                    fadeDelayMs={(idx % CURATED_SIZE) * 200}
                                />
                            ))}
                        </Box>
                    </Section>
                );
            })}
        </>
    );
};
