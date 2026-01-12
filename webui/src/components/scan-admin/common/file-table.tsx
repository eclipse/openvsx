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

import React, { FunctionComponent, useState } from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Checkbox,
    Typography,
    Chip,
} from '@mui/material';
import { FileDecision } from '../../../context/scan-admin';
import { ConditionalTooltip, formatDateTime } from './index';
import { useTheme } from '@mui/material/styles';

interface FileTableProps {
    files: FileDecision[];
    type: 'allowed' | 'blocked';
    selectedFiles?: Set<string>;
    onSelectionChange?: (fileIds: Set<string>) => void;
}

export const FileTable: FunctionComponent<FileTableProps> = (props) => {
    const { files, type, selectedFiles = new Set(), onSelectionChange } = props;
    const [internalSelection, setInternalSelection] = useState<Set<string>>(selectedFiles);
    const theme = useTheme();
    const [columnWidths, setColumnWidths] = useState({
        file: 250,
        type: 100,
        date: 200,
        actionBy: 130,
        extension: 250,
        publisher: 150,
        version: 100,
    });

    const selection = onSelectionChange ? selectedFiles : internalSelection;
    const setSelection = onSelectionChange || setInternalSelection;

    const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.checked) {
            const allIds = new Set(files.map(f => f.id));
            setSelection(allIds);
        } else {
            setSelection(new Set());
        }
    };

    const handleSelectOne = (fileId: string) => {
        const newSelection = new Set(selection);
        if (newSelection.has(fileId)) {
            newSelection.delete(fileId);
        } else {
            newSelection.add(fileId);
        }
        setSelection(newSelection);
    };

    const isSelected = (fileId: string) => selection.has(fileId);
    const allSelected = files.length > 0 && selection.size === files.length;
    const someSelected = selection.size > 0 && selection.size < files.length;

    const handleDoubleClick = (column: keyof typeof columnWidths, event: React.MouseEvent) => {
        event.preventDefault();

        const minWidth = 80;
        const columns: (keyof typeof columnWidths)[] = ['file', 'type', 'date', 'actionBy', 'extension', 'publisher', 'version'];
        const currentIndex = columns.indexOf(column);

        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        if (!context) return;

        context.font = '14px Roboto, sans-serif';

        let idealWidth = minWidth;
        const padding = 32;

        const headerTexts: Record<keyof typeof columnWidths, string> = {
            file: 'File',
            type: 'Type',
            date: type === 'allowed' ? 'Date Allowed' : 'Date Blocked',
            actionBy: type === 'allowed' ? 'Allowed By' : 'Blocked By',
            extension: 'Extension',
            publisher: 'Publisher',
            version: 'Version',
        };
        idealWidth = Math.max(idealWidth, context.measureText(headerTexts[column]).width + padding);

        files.forEach(file => {
            let text = '';
            switch (column) {
                case 'file': {
                    const fileNameWidth = context.measureText(file.fileName).width;
                    const fileHashWidth = context.measureText(file.fileHash).width;
                    idealWidth = Math.max(idealWidth, fileNameWidth + padding, fileHashWidth + padding);
                    return;
                }
                case 'type':
                    text = file.fileType;
                    break;
                case 'date': {
                    text = formatDateTime(file.dateDecided);
                    break;
                }
                case 'actionBy':
                    text = file.decidedBy;
                    break;
                case 'extension': {
                    const displayNameWidth = context.measureText(file.displayName).width;
                    const namespaceWidth = context.measureText(`${file.namespace}.${file.extensionName}`).width;
                    idealWidth = Math.max(idealWidth, displayNameWidth + padding, namespaceWidth + padding);
                    return;
                }
                case 'publisher':
                    text = file.publisher;
                    break;
                case 'version':
                    text = file.version;
                    break;
            }

            if (text) {
                const width = context.measureText(text).width + padding;
                idealWidth = Math.max(idealWidth, width);
            }
        });

        idealWidth = Math.min(idealWidth, 600);

        const currentWidth = columnWidths[column];
        const growthNeeded = idealWidth - currentWidth;

        if (growthNeeded <= 0) {
            setColumnWidths(prev => ({
                ...prev,
                [column]: idealWidth,
            }));
            return;
        }

        let availableSpace = 0;
        for (let i = currentIndex + 1; i < columns.length; i++) {
            const col = columns[i];
            availableSpace += columnWidths[col] - minWidth;
        }

        if (availableSpace <= 0) {
            return;
        }

        const actualGrowth = Math.min(growthNeeded, availableSpace);
        const newWidth = currentWidth + actualGrowth;

        const newWidths = { ...columnWidths };
        newWidths[column] = newWidth;

        let remainingShrink = actualGrowth;
        for (let i = currentIndex + 1; i < columns.length && remainingShrink > 0; i++) {
            const col = columns[i];
            const availableShrink = columnWidths[col] - minWidth;
            const shrinkAmount = Math.min(remainingShrink, availableShrink);
            newWidths[col] = columnWidths[col] - shrinkAmount;
            remainingShrink -= shrinkAmount;
        }

        setColumnWidths(newWidths);
    };

    const handleMouseDown = (column: keyof typeof columnWidths, event: React.MouseEvent) => {
        event.preventDefault();
        const startX = event.clientX;
        const startWidths = { ...columnWidths };
        const minWidth = 80;

        const columns: (keyof typeof columnWidths)[] = ['file', 'type', 'date', 'actionBy', 'extension', 'publisher', 'version'];
        const currentIndex = columns.indexOf(column);

        if (currentIndex === columns.length - 1) return;

        const handleMouseMove = (e: MouseEvent) => {
            const diff = e.clientX - startX;
            const newWidths = { ...startWidths };

            if (diff > 0) {
                let remainingDiff = diff;
                const currentGrowth = Math.min(remainingDiff, Number.MAX_SAFE_INTEGER);
                newWidths[column] = startWidths[column] + currentGrowth;

                for (let i = currentIndex + 1; i < columns.length && remainingDiff > 0; i++) {
                    const col = columns[i];
                    const availableShrink = startWidths[col] - minWidth;
                    const shrinkAmount = Math.min(remainingDiff, availableShrink);
                    newWidths[col] = startWidths[col] - shrinkAmount;
                    remainingDiff -= shrinkAmount;
                }

                newWidths[column] = startWidths[column] + (diff - remainingDiff);

            } else if (diff < 0) {
                let remainingDiff = Math.abs(diff);

                const currentShrink = Math.min(remainingDiff, startWidths[column] - minWidth);
                newWidths[column] = startWidths[column] - currentShrink;
                remainingDiff -= currentShrink;

                if (remainingDiff > 0) {
                    for (let i = currentIndex - 1; i >= 0 && remainingDiff > 0; i--) {
                        const col = columns[i];
                        const availableShrink = startWidths[col] - minWidth;
                        const shrinkAmount = Math.min(remainingDiff, availableShrink);
                        newWidths[col] = startWidths[col] - shrinkAmount;
                        remainingDiff -= shrinkAmount;
                    }
                }

                const totalShrunk = Math.abs(diff) - remainingDiff;
                newWidths[columns[currentIndex + 1]] = startWidths[columns[currentIndex + 1]] + totalShrunk;
            }

            setColumnWidths(newWidths);
        };

        const handleMouseUp = () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    };

    const cellStyle = {
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap' as const,
    };

    const truncateStyle = {
        display: 'block',
        ...cellStyle,
    };

    return (
        <TableContainer
            sx={{
                width: '100%',
                backgroundColor: theme.palette.scanBackground.default,
                borderRadius: 2,
                boxShadow: 2,
                overflowX: 'hidden',
            }}
        >
            <Table size='small' sx={{ tableLayout: 'fixed', width: '100%' }}>
                <TableHead>
                    <TableRow sx={{ height: '52px' }}>
                        <TableCell padding='checkbox' sx={{ width: 50 }}>
                            <Checkbox
                                indeterminate={someSelected}
                                checked={allSelected}
                                onChange={handleSelectAll}
                                sx={{
                                    color: theme.palette.scanBackground.light,
                                    '&.Mui-checked': {
                                        color: 'secondary.main',
                                    },
                                    '&.MuiCheckbox-indeterminate': {
                                        color: 'secondary.main',
                                    },
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.file, position: 'relative', ...cellStyle }}>
                            File
                            <div
                                onMouseDown={(e) => handleMouseDown('file', e)}
                                onDoubleClick={(e) => handleDoubleClick('file', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.type, position: 'relative', ...cellStyle }}>
                            Type
                            <div
                                onMouseDown={(e) => handleMouseDown('type', e)}
                                onDoubleClick={(e) => handleDoubleClick('type', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.date, position: 'relative', ...cellStyle }}>
                            {type === 'allowed' ? 'Date Allowed' : 'Date Blocked'}
                            <div
                                onMouseDown={(e) => handleMouseDown('date', e)}
                                onDoubleClick={(e) => handleDoubleClick('date', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.actionBy, position: 'relative', ...cellStyle }}>
                            {type === 'allowed' ? 'Allowed By' : 'Blocked By'}
                            <div
                                onMouseDown={(e) => handleMouseDown('actionBy', e)}
                                onDoubleClick={(e) => handleDoubleClick('actionBy', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.extension, position: 'relative', ...cellStyle }}>
                            Extension
                            <div
                                onMouseDown={(e) => handleMouseDown('extension', e)}
                                onDoubleClick={(e) => handleDoubleClick('extension', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.publisher, position: 'relative', ...cellStyle }}>
                            Publisher
                            <div
                                onMouseDown={(e) => handleMouseDown('publisher', e)}
                                onDoubleClick={(e) => handleDoubleClick('publisher', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                        <TableCell sx={{ width: columnWidths.version, position: 'relative', ...cellStyle }}>
                            Version
                            <div
                                onMouseDown={(e) => handleMouseDown('version', e)}
                                onDoubleClick={(e) => handleDoubleClick('version', e)}
                                style={{
                                    position: 'absolute',
                                    right: 0,
                                    top: 0,
                                    bottom: 0,
                                    width: '5px',
                                    cursor: 'col-resize',
                                    userSelect: 'none',
                                }}
                            />
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {files.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={8} align='center'>
                                <Typography variant='body2' color='text.secondary' sx={{ py: 4 }}>
                                    No {type} files
                                </Typography>
                            </TableCell>
                        </TableRow>
                    ) : (
                        files.map((file) => {
                            const selected = isSelected(file.id);
                            const dateField = file.dateDecided;
                            const actionByField = file.decidedBy;

                            return (
                                <TableRow
                                    key={file.id}
                                    hover
                                    onClick={() => handleSelectOne(file.id)}
                                    selected={selected}
                                    sx={{
                                        cursor: 'pointer',
                                        '&.Mui-selected': {
                                            backgroundColor: theme.palette.selected.background,
                                        },
                                        '&.Mui-selected:hover': {
                                            backgroundColor: theme.palette.selected.backgroundHover,
                                        },
                                    }}
                                >
                                    <TableCell padding='checkbox'>
                                        <Checkbox
                                            checked={selected}
                                            sx={{
                                                color: theme.palette.scanBackground.light,
                                                '&.Mui-checked': {
                                                    color: 'secondary.main',
                                                },
                                            }}
                                        />
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.file, ...cellStyle }}>
                                        <ConditionalTooltip title={file.fileName} arrow>
                                            <Typography variant='body2' sx={{ fontFamily: 'monospace', ...truncateStyle }}>
                                                {file.fileName}
                                            </Typography>
                                        </ConditionalTooltip>
                                        <ConditionalTooltip title={file.fileHash} arrow>
                                            <Typography
                                                variant='caption'
                                                sx={{
                                                    fontFamily: 'monospace',
                                                    fontSize: '0.7rem',
                                                    color: 'text.secondary',
                                                    ...truncateStyle,
                                                }}
                                            >
                                                {file.fileHash}
                                            </Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.type, ...cellStyle }}>
                                        <Chip label={file.fileType} size='small' variant='outlined' />
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.date, ...cellStyle }}>
                                        <ConditionalTooltip title={formatDateTime(dateField)} arrow>
                                            <Typography variant='body2' sx={{ fontSize: '0.8rem', ...truncateStyle }}>
                                                {formatDateTime(dateField)}
                                            </Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.actionBy, ...cellStyle }}>
                                        <ConditionalTooltip title={actionByField} arrow>
                                            <Typography variant='body2' sx={truncateStyle}>
                                                {actionByField}
                                            </Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.extension, ...cellStyle }}>
                                        <ConditionalTooltip title={file.displayName} arrow>
                                            <Typography variant='body2' sx={truncateStyle}>{file.displayName}</Typography>
                                        </ConditionalTooltip>
                                        <ConditionalTooltip title={`${file.namespace}.${file.extensionName}`} arrow>
                                            <Typography variant='caption' sx={{ color: 'text.secondary', ...truncateStyle }}>
                                                {file.namespace}.{file.extensionName}
                                            </Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.publisher, ...cellStyle }}>
                                        <ConditionalTooltip title={file.publisher} arrow>
                                            <Typography variant='body2' sx={truncateStyle}>{file.publisher}</Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                    <TableCell sx={{ width: columnWidths.version, ...cellStyle }}>
                                        <ConditionalTooltip title={file.version} arrow>
                                            <Typography variant='body2' sx={truncateStyle}>{file.version}</Typography>
                                        </ConditionalTooltip>
                                    </TableCell>
                                </TableRow>
                            );
                        })
                    )}
                </TableBody>
            </Table>
        </TableContainer>
    );
};
