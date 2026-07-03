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
    IconButton,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableFooter,
    TableHead,
    TablePagination,
    TableRow,
    Typography
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { VersionTargetPlatforms } from '../../extension-registry-types';

const PAGE_SIZE = 20;

export const ExtensionVersionTable: FunctionComponent<ExtensionVersionTableProps> = ({
    versions,
    page,
    onPageChange,
    onDeleteVersion
}) => {
    const pagedVersions = versions.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

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
                    {pagedVersions.map(v => (
                        <TableRow key={v.version}>
                            <TableCell sx={{ whiteSpace: 'nowrap' }}>{v.version}</TableCell>
                            <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                {v.targetPlatforms.map((tp, index) => (
                                    <Typography key={tp.targetPlatform} component='span'>
                                        <Typography
                                            component='span'
                                            sx={{ textDecoration: tp.active ? 'none' : 'line-through' }}>
                                            {tp.targetPlatform}
                                        </Typography>
                                        {index < v.targetPlatforms.length - 1 ? ', ' : ''}
                                    </Typography>
                                ))}
                            </TableCell>
                            <TableCell align='right' padding='checkbox' sx={{ whiteSpace: 'nowrap' }}>
                                <IconButton size='small' title='Delete version' onClick={() => onDeleteVersion(v)}>
                                    <DeleteIcon fontSize='small' color='error' />
                                </IconButton>
                            </TableCell>
                        </TableRow>
                    ))}
                    {versions.length === 0 && (
                        <TableRow>
                            <TableCell colSpan={3} align='center'>
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
}
