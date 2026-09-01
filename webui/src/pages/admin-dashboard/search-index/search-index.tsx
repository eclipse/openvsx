/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { FC } from 'react';
import { Alert, Box, Button, Paper, Typography } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { ButtonWithProgress } from '../../../components/button-with-progress';
import type { SearchIndex } from '../../../extension-registry-types';
import { handleError } from '../../../utils';
import { useRefreshSearchIndex, useSearchIndex, useUpdateSearchIndex } from './use-search-index';

/**
 * Admin dashboard view of the search index: what the index holds against what it is built from, and a
 * rebuild. The two counts side by side are the point - an index that has quietly lost entries answers
 * searches perfectly well, just with nothing in them, which is indistinguishable from an empty registry
 * unless you can see both numbers at once.
 */
export const SearchIndexAdmin: FC = () => {
    const { data, isLoading, error } = useSearchIndex();
    const refresh = useRefreshSearchIndex();
    const update = useUpdateSearchIndex();

    return (
        <Box sx={{ p: 3 }}>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mb: 3,
                    flexWrap: 'wrap',
                    gap: 2
                }}>
                <Box>
                    <Typography variant='h4' component='h1'>
                        Search Index
                    </Typography>
                    <Typography variant='body2' color='text.secondary'>
                        The state of the index that answers extension searches, and a full rebuild of it from the
                        database.
                    </Typography>
                </Box>
                <Button variant='outlined' startIcon={<RefreshIcon />} onClick={refresh}>
                    Refresh
                </Button>
            </Box>

            {error && <Alert severity='error'>{handleError(error)}</Alert>}
            {update.error && (
                <Alert severity='error' sx={{ mb: 2 }}>
                    {handleError(update.error)}
                </Alert>
            )}
            {update.isSuccess && (
                <Alert severity='success' sx={{ mb: 2 }}>
                    Rebuilt the search index.
                </Alert>
            )}

            {!error && !isLoading && data && (
                <>
                    <SearchIndexHealth index={data} />
                    <SearchIndexStatistics index={data} />
                    {/* Only elasticsearch has an index to rebuild. The database engine's updateSearchIndex
                        merely evicts a cache, so offering the button there would report a rebuild that did
                        not happen. */}
                    {canRebuild(data) && (
                        <Paper sx={{ p: 3 }}>
                            <Typography variant='subtitle1'>Rebuild the index</Typography>
                            <Typography variant='body2' color='text.secondary' sx={{ mb: 2 }}>
                                Deletes the index and builds it again from every active extension. Searching keeps
                                working while it runs, but returns incomplete results until it finishes.
                            </Typography>
                            <ButtonWithProgress
                                working={update.isPending}
                                onClick={() => update.mutate()}
                                title='Delete and rebuild the search index'>
                                Update search index
                            </ButtonWithProgress>
                        </Paper>
                    )}
                </>
            )}
        </Box>
    );
};

const canRebuild = (index: SearchIndex) => index.enabled && index.implementation === 'elasticsearch';

/**
 * Says whether the two counts agree, so nobody has to subtract them by eye. Only elasticsearch has an
 * index to disagree about; the database engine reads the tables a search would read anyway.
 */
const SearchIndexHealth: FC<{ index: SearchIndex }> = ({ index }) => {
    if (!index.enabled) {
        return (
            <Alert severity='warning' sx={{ mb: 2 }}>
                Searching is disabled on this registry.
            </Alert>
        );
    }
    if (index.implementation !== 'elasticsearch') {
        return (
            <Alert severity='info' sx={{ mb: 2 }}>
                Searches are answered from the database, so there is no index to report on or rebuild.
            </Alert>
        );
    }
    if (!index.indexExists) {
        return (
            <Alert severity='error' sx={{ mb: 2 }}>
                The index does not exist. Searching returns nothing until it is built.
            </Alert>
        );
    }

    const indexed = index.indexedDocuments ?? 0;
    const missing = index.activeExtensions - indexed;
    if (missing > 0) {
        return (
            <Alert severity='error' sx={{ mb: 2 }}>
                {missing.toLocaleString()} active {missing === 1 ? 'extension is' : 'extensions are'} missing from the
                index and cannot be found by searching. Rebuild it below.
            </Alert>
        );
    }
    if (missing < 0) {
        return (
            <Alert severity='warning' sx={{ mb: 2 }}>
                The index holds {(-missing).toLocaleString()} more {-missing === 1 ? 'entry' : 'entries'} than there are
                active extensions, so it lists some that no longer exist. Rebuild it below.
            </Alert>
        );
    }
    return (
        <Alert severity='success' sx={{ mb: 2 }}>
            Every active extension is indexed.
        </Alert>
    );
};

const SearchIndexStatistics: FC<{ index: SearchIndex }> = ({ index }) => (
    <Paper sx={{ p: 3, mb: 2 }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
            <Statistic label='Search engine' value={index.implementation} />
            <Statistic
                label='Indexed extensions'
                value={index.indexedDocuments !== undefined ? index.indexedDocuments.toLocaleString() : '—'}
            />
            <Statistic label='Active extensions' value={index.activeExtensions.toLocaleString()} />
            {index.maxResultWindow !== undefined && (
                <Statistic label='Max result window' value={index.maxResultWindow.toLocaleString()} />
            )}
        </Box>
    </Paper>
);

const Statistic: FC<{ label: string; value: string }> = ({ label, value }) => (
    <Box>
        <Typography variant='body2' color='text.secondary'>
            {label}
        </Typography>
        <Typography variant='h5' component='p'>
            {value}
        </Typography>
    </Box>
);
