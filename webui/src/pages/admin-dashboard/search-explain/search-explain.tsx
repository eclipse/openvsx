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

import { FC, Fragment, FormEvent, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Chip,
    Collapse,
    IconButton,
    LinearProgress,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Tooltip,
    Typography
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import type { SearchExplainEntry } from '../../../extension-registry-types';
import { ScoreBreakdown } from './score-breakdown';
import { formatCompactNumber, handleError } from '../../../utils';
import { useSearchExplain } from './use-search-explain';

const RESULT_SIZE = 25;

const num = (value: number | undefined, digits = 3): string =>
    value === undefined || value === null ? '—' : value.toFixed(digits);

/**
 * How much of the relevance each term accounts for, drawn as one bar.
 * <p>
 * The numbers alone make you do arithmetic across a row to see which term decided the ordering; the bar
 * says it at a glance, which is the question this page exists to answer.
 */
const RelevanceBar: FC<{ entry: SearchExplainEntry }> = ({ entry }) => {
    const parts = [
        { key: 'rating', value: entry.rating ?? 0, color: 'primary.main' },
        { key: 'downloads', value: entry.downloads ?? 0, color: 'success.main' },
        { key: 'recency', value: entry.recency ?? 0, color: 'warning.main' }
    ];
    const total = parts.reduce((sum, part) => sum + part.value, 0);
    if (total === 0) {
        return <Box sx={{ color: 'text.disabled', fontSize: '0.75rem' }}>—</Box>;
    }

    return (
        <Tooltip title={parts.map(part => `${part.key} ${part.value.toFixed(3)}`).join('  ·  ')} placement='top' arrow>
            <Box sx={{ display: 'flex', height: 10, borderRadius: 1, overflow: 'hidden', minWidth: 120 }}>
                {parts.map(part => (
                    <Box key={part.key} sx={{ width: `${(part.value / total) * 100}%`, bgcolor: part.color }} />
                ))}
            </Box>
        </Tooltip>
    );
};

/**
 * Why a search returned what it returned.
 * <p>
 * A result's score is two numbers multiplied: how well the query matched the extension's text, and a
 * relevance computed from its rating, downloads and age when it was indexed. Only the product reaches the
 * result list, so a ranking that looks wrong gives no clue which half is responsible - and the two want
 * entirely different fixes. One is the query's field weights, the other the relevance formula. This shows
 * both, for the search the registry actually runs rather than a reconstruction of it.
 */
export const SearchExplainAdmin: FC = () => {
    const [term, setTerm] = useState('');
    const [submitted, setSubmitted] = useState('');
    const [expanded, setExpanded] = useState<string | undefined>(undefined);
    const { data, isFetching, error } = useSearchExplain(submitted, RESULT_SIZE);

    const submit = (event: FormEvent) => {
        event.preventDefault();
        setSubmitted(term.trim());
    };

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ mb: 3 }}>
                <Typography variant='h4' component='h1'>
                    Search Explain
                </Typography>
                <Typography variant='body2' color='text.secondary'>
                    Run a search and see what each result&apos;s score is made of. The score is the text match
                    multiplied by the stored relevance, so a result in the wrong place is either a query-weighting
                    problem or a relevance-formula one — this says which.
                </Typography>
            </Box>

            <Box component='form' onSubmit={submit} sx={{ display: 'flex', gap: 2, mb: 3, maxWidth: 640 }}>
                <TextField
                    fullWidth
                    size='small'
                    label='Search term'
                    placeholder='markdown'
                    value={term}
                    onChange={event => setTerm(event.target.value)}
                />
                <Button type='submit' variant='contained' disabled={term.trim().length === 0 || isFetching}>
                    Explain
                </Button>
            </Box>

            {error && <Alert severity='error'>{handleError(error)}</Alert>}
            {isFetching && <LinearProgress sx={{ mb: 2 }} />}

            {data && (
                <>
                    <Alert severity='info' sx={{ mb: 2 }}>
                        {data.totalHits.toLocaleString()} matches for <strong>{data.query}</strong>, showing the first{' '}
                        {data.entries.length}. Every downloads term is measured against the registry&apos;s largest
                        count, <strong>{formatCompactNumber(data.references.maxDownloadCount)}</strong>, and every
                        rating is smoothed towards the registry average of{' '}
                        <strong>{data.references.averageReviewRating.toFixed(2)}</strong> — which is why a term can
                        contribute nothing for reasons that have nothing to do with the extension.
                    </Alert>

                    <TableContainer component={Paper} elevation={0} variant='outlined'>
                        <Table size='small'>
                            <TableHead>
                                <TableRow>
                                    <TableCell />
                                    <TableCell>#</TableCell>
                                    <TableCell>Extension</TableCell>
                                    <TableCell align='right'>Score</TableCell>
                                    <TableCell align='right'>Text</TableCell>
                                    <TableCell align='right'>Relevance</TableCell>
                                    <TableCell>Relevance made of</TableCell>
                                    <TableCell />
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {data.entries.map(entry => (
                                    <Fragment key={`${entry.namespace}.${entry.name}`}>
                                        <TableRow
                                            hover
                                            sx={{ cursor: 'pointer', '& > *': { borderBottom: 'unset' } }}
                                            onClick={() =>
                                                setExpanded(
                                                    expanded === `${entry.namespace}.${entry.name}`
                                                        ? undefined
                                                        : `${entry.namespace}.${entry.name}`
                                                )
                                            }>
                                            <TableCell sx={{ width: 32 }}>
                                                <IconButton size='small' aria-label='Show the score breakdown'>
                                                    {expanded === `${entry.namespace}.${entry.name}` ? (
                                                        <KeyboardArrowUpIcon fontSize='inherit' />
                                                    ) : (
                                                        <KeyboardArrowDownIcon fontSize='inherit' />
                                                    )}
                                                </IconButton>
                                            </TableCell>
                                            <TableCell>{entry.position + 1}</TableCell>
                                            <TableCell>
                                                <Box sx={{ fontWeight: 500 }}>
                                                    {entry.namespace}.{entry.name}
                                                </Box>
                                                <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                                                    {formatCompactNumber(entry.downloadCount)} downloads
                                                    {entry.timestamp ? ` · ${entry.timestamp.slice(0, 10)}` : ''}
                                                </Box>
                                            </TableCell>
                                            <TableCell align='right'>{num(entry.score)}</TableCell>
                                            <TableCell align='right'>{num(entry.textScore)}</TableCell>
                                            <TableCell align='right'>
                                                {num(entry.storedRelevance)}
                                                {/* A stored relevance the formula no longer agrees with means the
                                                index predates a change to it - worth seeing before drawing any
                                                conclusion from the ordering. */}
                                                {entry.currentRelevance !== undefined &&
                                                    Math.abs(entry.currentRelevance - entry.storedRelevance) > 0.01 && (
                                                        <Tooltip
                                                            title={`Recomputes to ${entry.currentRelevance.toFixed(
                                                                3
                                                            )} — the index is older than the formula`}
                                                            arrow>
                                                            <Box
                                                                component='span'
                                                                sx={{ ml: 0.5, color: 'warning.main', cursor: 'help' }}>
                                                                ≠
                                                            </Box>
                                                        </Tooltip>
                                                    )}
                                            </TableCell>
                                            <TableCell>
                                                <RelevanceBar entry={entry} />
                                            </TableCell>
                                            <TableCell>
                                                {entry.unverified && (
                                                    <Chip size='small' label='unverified' sx={{ mr: 0.5 }} />
                                                )}
                                                {entry.deprecated && <Chip size='small' label='deprecated' />}
                                            </TableCell>
                                        </TableRow>
                                        <TableRow>
                                            <TableCell
                                                colSpan={7}
                                                sx={{ p: 0, borderBottom: '1px solid', borderColor: 'divider' }}>
                                                <Collapse
                                                    in={expanded === `${entry.namespace}.${entry.name}`}
                                                    timeout='auto'
                                                    unmountOnExit>
                                                    <ScoreBreakdown entry={entry} />
                                                </Collapse>
                                            </TableCell>
                                        </TableRow>
                                    </Fragment>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </>
            )}
        </Box>
    );
};
