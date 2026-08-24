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

import { FC, useState } from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    List,
    ListItem,
    ListItemText,
    Paper,
    Typography
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import RefreshIcon from '@mui/icons-material/Refresh';
import { ButtonWithProgress } from '../../../components/button-with-progress';
import type { ConsistencyCheck } from '../../../extension-registry-types';
import { handleError } from '../../../utils';
import {
    useConsistencyChecks,
    useConsistencyFindings,
    useFixAllConsistencyFindings,
    useFixConsistencyFinding,
    useRefreshConsistency
} from './use-consistency';

/**
 * Admin dashboard overview of every registered data-consistency check (#1622): each check's live
 * finding count, and actions to fix them - one at a time or all at once. A check that does not require
 * human judgment also auto-fixes itself once a day via a scheduled job; every fix, scheduled or manual,
 * shows up on the Admin Logs page rather than a bespoke history view here.
 */
export const DataConsistency: FC = () => {
    const { data, isLoading, error } = useConsistencyChecks();
    const refresh = useRefreshConsistency();
    const [expanded, setExpanded] = useState<Set<string>>(new Set());

    const toggleExpanded = (checkId: string) => {
        setExpanded(prev => {
            const next = new Set(prev);
            if (next.has(checkId)) {
                next.delete(checkId);
            } else {
                next.add(checkId);
            }
            return next;
        });
    };

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
                        Data Consistency
                    </Typography>
                    <Typography variant='body2' color='text.secondary'>
                        Checks the database for known inconsistencies and lets you fix them directly. Findings shown
                        below are always live; a check that does not require human judgment also auto-fixes itself once
                        a day via a scheduled job.
                    </Typography>
                </Box>
                <Button variant='outlined' startIcon={<RefreshIcon />} onClick={refresh}>
                    Refresh
                </Button>
            </Box>

            {error && <Alert severity='error'>{handleError(error)}</Alert>}

            {!error &&
                !isLoading &&
                data?.checks.map(check => (
                    <ConsistencyCheckCard
                        key={check.id}
                        check={check}
                        expanded={expanded.has(check.id)}
                        onToggle={() => toggleExpanded(check.id)}
                    />
                ))}
        </Box>
    );
};

const ConsistencyCheckCard: FC<{ check: ConsistencyCheck; expanded: boolean; onToggle: () => void }> = ({
    check,
    expanded,
    onToggle
}) => {
    const { data, isLoading, error } = useConsistencyFindings(check.id, expanded);
    const fixAll = useFixAllConsistencyFindings(check.id);
    const fixOne = useFixConsistencyFinding(check.id);

    const healthy = check.currentFindingsCount === 0;

    return (
        <Accordion expanded={expanded} onChange={onToggle} sx={{ mb: 1 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%', pr: 2 }}>
                    <Chip
                        label={healthy ? 'OK' : `${check.currentFindingsCount} found`}
                        color={healthy ? 'success' : 'error'}
                        size='small'
                    />
                    <Box sx={{ flex: 1 }}>
                        <Typography variant='subtitle1'>{check.name}</Typography>
                        <Typography variant='body2' color='text.secondary'>
                            {check.description}
                        </Typography>
                    </Box>
                    {!healthy && (
                        <ButtonWithProgress
                            working={fixAll.isPending}
                            onClick={event => {
                                // Fixing must not also toggle the accordion the button sits inside of.
                                event.stopPropagation();
                                fixAll.mutate();
                            }}>
                            Fix all
                        </ButtonWithProgress>
                    )}
                </Box>
            </AccordionSummary>
            <AccordionDetails>
                {fixAll.isError && <Alert severity='error'>{handleError(fixAll.error)}</Alert>}
                {fixOne.isError && <Alert severity='error'>{handleError(fixOne.error)}</Alert>}
                {error && <Alert severity='error'>{handleError(error)}</Alert>}

                {!error &&
                    !isLoading &&
                    data &&
                    (data.findings.length === 0 ? (
                        <Typography color='text.secondary'>No inconsistencies found.</Typography>
                    ) : (
                        <Paper variant='outlined'>
                            <List dense>
                                {data.findings.map(finding => (
                                    <ListItem
                                        key={finding.entityId}
                                        secondaryAction={
                                            <ButtonWithProgress
                                                working={fixOne.isPending && fixOne.variables === finding.entityId}
                                                onClick={() => fixOne.mutate(finding.entityId)}>
                                                Fix
                                            </ButtonWithProgress>
                                        }>
                                        <ListItemText primary={finding.label} secondary={finding.detail} />
                                    </ListItem>
                                ))}
                            </List>
                        </Paper>
                    ))}
            </AccordionDetails>
        </Accordion>
    );
};
