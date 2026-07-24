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

import { FunctionComponent } from 'react';
import {
    Chip,
    IconButton,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableFooter,
    TableHead,
    TablePagination,
    TableRow
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import { VersionTargetPlatforms } from '../../extension-registry-types';
import { getTargetPlatformDisplayName } from '../../utils';

const PAGE_SIZE = 20;

export const ExtensionVersionTable: FunctionComponent<ExtensionVersionTableProps> = ({
    versions,
    page,
    onPageChange,
    onDeleteVersion,
    onPurgeVersion
}) => {
    const pagedVersions = versions.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);
    const columnCount = onPurgeVersion ? 4 : 3;

    return (
        <TableContainer component={Paper} variant='outlined'>
            <Table size='small'>
                <TableHead>
                    <TableRow>
                        <TableCell width={'auto'} sx={{ whiteSpace: 'nowrap' }}>
                            Version
                        </TableCell>
                        <TableCell>Target Platforms</TableCell>
                        <TableCell />
                    </TableRow>
                </TableHead>
                <TableBody>
                    {pagedVersions.map(v => {
                        const allRemoved = v.targetPlatforms.length > 0 && v.targetPlatforms.every(tp => tp.removed);
                        // canDelete is only populated in the user settings view; undefined means unrestricted.
                        const notPublisher = v.canDelete === false;
                        const deleteDisabled = allRemoved || notPublisher;
                        const deleteTitle = notPublisher
                            ? 'Only the publisher or a namespace owner can delete this version'
                            : allRemoved
                              ? 'Version already removed'
                              : 'Delete version';
                        return (
                            <TableRow key={v.version}>
                                <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                    <Stack direction='row' spacing={1} alignItems='center'>
                                        <span>{v.version}</span>
                                        {allRemoved && (
                                            <Chip label='Removed' size='small' color='default' variant='outlined' />
                                        )}
                                    </Stack>
                                </TableCell>
                                <TableCell>
                                    <Stack direction='row' spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                                        {v.targetPlatforms.map(tp => (
                                            <Chip
                                                key={tp.targetPlatform}
                                                size='small'
                                                variant='outlined'
                                                color={tp.removed ? 'warning' : tp.active ? 'primary' : 'default'}
                                                title={tp.removed ? 'Removed' : tp.active ? undefined : 'Inactive'}
                                                label={
                                                    getTargetPlatformDisplayName(tp.targetPlatform) || tp.targetPlatform
                                                }
                                                sx={{ textDecoration: tp.active ? 'none' : 'line-through' }}
                                            />
                                        ))}
                                    </Stack>
                                </TableCell>
                                <TableCell align='right' padding='checkbox' sx={{ whiteSpace: 'nowrap' }}>
                                    <IconButton
                                        size='small'
                                        title={deleteTitle}
                                        disabled={deleteDisabled}
                                        onClick={() => onDeleteVersion(v)}>
                                        <DeleteIcon fontSize='small' color={deleteDisabled ? 'disabled' : 'error'} />
                                    </IconButton>
                                </TableCell>
                                {onPurgeVersion && (
                                    <TableCell align='right' padding='checkbox' sx={{ whiteSpace: 'nowrap' }}>
                                        <IconButton
                                            size='small'
                                            title='Purge version permanently'
                                            onClick={() => onPurgeVersion(v)}>
                                            <DeleteForeverIcon fontSize='small' color='error' />
                                        </IconButton>
                                    </TableCell>
                                )}
                            </TableRow>
                        );
                    })}
                    {versions.length === 0 && (
                        <TableRow>
                            <TableCell colSpan={columnCount} align='center'>
                                No version information available.
                            </TableCell>
                        </TableRow>
                    )}
                </TableBody>
                <TableFooter>
                    <TableRow>
                        <TablePagination
                            count={versions.length}
                            page={page}
                            rowsPerPage={PAGE_SIZE}
                            rowsPerPageOptions={[PAGE_SIZE]}
                            onPageChange={(_, newPage) => onPageChange(newPage)}
                        />
                    </TableRow>
                </TableFooter>
            </Table>
        </TableContainer>
    );
};

export interface ExtensionVersionTableProps {
    versions: VersionTargetPlatforms[];
    page: number;
    onPageChange: (page: number) => void;
    onDeleteVersion: (version: VersionTargetPlatforms) => void;
    // When provided (admin only), each row shows a permanent-purge action.
    onPurgeVersion?: (version: VersionTargetPlatforms) => void;
}
