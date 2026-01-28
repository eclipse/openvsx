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

import React from 'react';
import { Box, Typography, Link, IconButton, Tooltip } from '@mui/material';
import {
    Check as CheckIcon,
    Warning as WarningAmberIcon,
} from '@mui/icons-material';
import { ScanResult } from '../../../context/scan-admin';
import { ConditionalTooltip, formatDateTime, formatDuration } from '../common';
import { useTheme } from '@mui/material/styles';
import {
    isRunning,
    hasDownload,
    getFileName,
} from './utils';

interface ScanCardContentProps {
    scan: ScanResult;
    showCheckbox?: boolean;
    checked?: boolean;
    onCheckboxChange?: (id: string, checked: boolean) => void;
    liveDuration: string;
}

/**
 * Content section of the ScanCard containing:
 * - Publisher, Version, Download (Row 2)
 * - Scan Start, Scan End, Duration, Decision Status (Row 3)
 * - Checkbox for selection
 */
export const ScanCardContent: React.FC<ScanCardContentProps> = ({
    scan,
    showCheckbox,
    checked,
    onCheckboxChange,
    liveDuration,
}) => {
    const theme = useTheme();
    const [isCheckboxHovering, setIsCheckboxHovering] = React.useState(false);

    return (
        <>
            {/* ROW 2 - Publisher, Version, Download, Checkbox */}
            {/* Column 1: Empty (below icon) */}
            <Box sx={{ gridRow: '2', gridColumn: '1' }} />

            {/* Column 2: Publisher */}
            <Box sx={{ gridRow: '2', gridColumn: '2', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Publisher
                </Typography>
                <ConditionalTooltip title={scan.publisher} arrow>
                    <Box
                        sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        <Link
                            href={scan.publisherUrl || undefined}
                            target='_blank'
                            rel='noopener noreferrer'
                            variant='body2'
                            sx={{
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                display: 'block',
                            }}
                        >
                            {scan.publisher}
                        </Link>
                    </Box>
                </ConditionalTooltip>
            </Box>

            {/* Column 3: Version */}
            <Box sx={{ gridRow: '2', gridColumn: '3', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Version
                </Typography>
                <ConditionalTooltip title={scan.version} arrow>
                    <Typography
                        variant='body2'
                        sx={{
                            display: 'block',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {scan.version}
                    </Typography>
                </ConditionalTooltip>
            </Box>

            {/* Column 4: Download */}
            <Box sx={{ gridRow: '2', gridColumn: '4', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Download
                </Typography>
                {isRunning(scan.status) ? (
                    <Typography variant='body2' color='text.disabled'>
                        N/A
                    </Typography>
                ) : hasDownload(scan) && scan.downloadUrl ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0 }}>
                        {scan.status === 'QUARANTINED' && (
                            <Tooltip
                                title='Potentially malicious'
                                arrow
                                disableInteractive
                                PopperProps={{
                                    disablePortal: true,
                                    sx: { pointerEvents: 'none' },
                                }}
                            >
                                <WarningAmberIcon
                                    sx={{
                                        fontSize: 16,
                                        color: theme.palette.quarantined.dark,
                                        flexShrink: 0,
                                    }}
                                />
                            </Tooltip>
                        )}
                        <ConditionalTooltip title={getFileName(scan.downloadUrl)} arrow>
                            <Link
                                href={scan.downloadUrl}
                                variant='body2'
                                sx={{
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    minWidth: 0,
                                    display: 'block',
                                    fontSize: '0.875rem',
                                }}
                            >
                                {getFileName(scan.downloadUrl)}
                            </Link>
                        </ConditionalTooltip>
                    </Box>
                ) : (
                    <Typography variant='body2' color='text.disabled'>
                        N/A
                    </Typography>
                )}
            </Box>

            {/* Column 5: Checkbox */}
            <Box sx={{ gridRow: '2', gridColumn: '5', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', minWidth: 0 }}>
                {showCheckbox && (
                    <IconButton
                        onClick={() => onCheckboxChange?.(scan.id, !checked)}
                        onMouseEnter={() => setIsCheckboxHovering(true)}
                        onMouseLeave={() => setIsCheckboxHovering(false)}
                        disableRipple
                        sx={{
                            padding: 0,
                            width: 36,
                            height: 36,
                            backgroundColor: 'transparent',
                        }}
                    >
                        <Box
                            className='checkbox-circle'
                            sx={{
                                position: 'relative',
                                width: 36,
                                height: 36,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <Box
                                className='checkbox-circle-outline'
                                sx={{
                                    position: 'absolute',
                                    width: 36,
                                    height: 36,
                                    borderRadius: '50%',
                                    border: checked ? 'none' : `2px solid ${isCheckboxHovering ? theme.palette.selected.border : theme.palette.scanBackground.light}`,
                                    backgroundColor: checked
                                        ? 'secondary.main'
                                        : isCheckboxHovering
                                            ? theme.palette.selected.background
                                            : 'transparent',
                                    transition: 'border-color 0.2s, background-color 0.2s',
                                }}
                            />
                            <CheckIcon
                                className='checkbox-icon'
                                sx={{
                                    fontSize: 24,
                                    color: checked
                                        ? 'white'
                                        : isCheckboxHovering
                                            ? theme.palette.selected.border
                                            : theme.palette.scanBackground.light,
                                    position: 'relative',
                                    zIndex: 1,
                                    transition: 'color 0.2s',
                                }}
                            />
                        </Box>
                    </IconButton>
                )}
            </Box>

            {/* ROW 3 - Scan Start, Scan End, Scan Duration, Decision Status */}
            {/* Column 1: Empty (below icon) */}
            <Box sx={{ gridRow: '3', gridColumn: '1' }} />

            {/* Column 2: Scan Start */}
            <Box sx={{ gridRow: '3', gridColumn: '2', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Scan Start
                </Typography>
                <ConditionalTooltip title={formatDateTime(scan.dateScanStarted)} arrow>
                    <Typography
                        variant='body2'
                        sx={{
                            display: 'block',
                            fontSize: '0.8rem',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {formatDateTime(scan.dateScanStarted)}
                    </Typography>
                </ConditionalTooltip>
            </Box>

            {/* Column 3: Scan End */}
            <Box sx={{ gridRow: '3', gridColumn: '3', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Scan End
                </Typography>
                {isRunning(scan.status) ? (
                    <ConditionalTooltip title={`${scan.status}...`} arrow>
                        <Typography
                            variant='body2'
                            sx={{
                                display: 'block',
                                background: theme.palette.gray.gradient,
                                backgroundSize: '200% 100%',
                                backgroundClip: 'text',
                                WebkitBackgroundClip: 'text',
                                color: 'transparent',
                                animation: 'shimmer 2s infinite',
                                '@keyframes shimmer': {
                                    '0%': { backgroundPosition: '200% 0' },
                                    '100%': { backgroundPosition: '-200% 0' },
                                },
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {scan.status}...
                        </Typography>
                    </ConditionalTooltip>
                ) : scan.dateScanEnded ? (
                    <ConditionalTooltip title={formatDateTime(scan.dateScanEnded)} arrow>
                        <Typography
                            variant='body2'
                            sx={{
                                display: 'block',
                                fontSize: '0.8rem',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {formatDateTime(scan.dateScanEnded)}
                        </Typography>
                    </ConditionalTooltip>
                ) : (
                    <Typography variant='body2' color='text.disabled'>
                        N/A
                    </Typography>
                )}
            </Box>

            {/* Column 4: Scan Duration */}
            <Box sx={{ gridRow: '3', gridColumn: '4', minWidth: 0 }}>
                <Typography
                    variant='caption'
                    color='text.secondary'
                    display='block'
                    sx={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    Scan Duration
                </Typography>
                {isRunning(scan.status) ? (
                    <ConditionalTooltip title={liveDuration} arrow>
                        <Typography
                            variant='body2'
                            sx={{
                                display: 'block',
                                background: theme.palette.gray.gradient,
                                backgroundSize: '200% 100%',
                                backgroundClip: 'text',
                                WebkitBackgroundClip: 'text',
                                color: 'transparent',
                                animation: 'shimmer 2s infinite',
                                '@keyframes shimmer': {
                                    '0%': { backgroundPosition: '200% 0' },
                                    '100%': { backgroundPosition: '-200% 0' },
                                },
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {liveDuration}
                        </Typography>
                    </ConditionalTooltip>
                ) : (
                    <ConditionalTooltip title={formatDuration(scan.dateScanStarted, scan.dateScanEnded || undefined)} arrow>
                        <Typography
                            variant='body2'
                            sx={{
                                display: 'block',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {formatDuration(scan.dateScanStarted, scan.dateScanEnded || undefined)}
                        </Typography>
                    </ConditionalTooltip>
                )}
            </Box>

            {/* Column 5: Decision Status */}
            {scan.status === 'QUARANTINED' && scan.adminDecision && (
                <Box sx={{
                    gridRow: '3',
                    gridColumn: '5',
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignSelf: 'end',
                    minWidth: 0,
                }}>
                    <Tooltip
                        title={`Decided by ${scan.adminDecision.decidedBy} on ${formatDateTime(scan.adminDecision.dateDecided)}`}
                        arrow
                        disableInteractive
                        PopperProps={{
                            disablePortal: true,
                            sx: { pointerEvents: 'none' },
                        }}
                    >
                        <Typography
                            variant='h6'
                            sx={{
                                fontWeight: 700,
                                color: scan.adminDecision.decision.toLowerCase() === 'allowed' ? theme.palette.allowed : theme.palette.blocked,
                                whiteSpace: 'nowrap',
                                cursor: 'help',
                            }}
                        >
                            {scan.adminDecision.decision.toLowerCase() === 'allowed' ? 'ALLOWED' : 'BLOCKED'}
                        </Typography>
                    </Tooltip>
                </Box>
            )}
            {scan.status === 'QUARANTINED' && !scan.adminDecision && (
                <Box sx={{
                    gridRow: '3',
                    gridColumn: '5',
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignSelf: 'end',
                    minWidth: 0,
                }}>
                    <Typography
                        variant='h6'
                        sx={{
                            fontWeight: 700,
                            color: theme.palette.review,
                            whiteSpace: 'nowrap',
                        }}
                    >
                        NEEDS REVIEW
                    </Typography>
                </Box>
            )}
        </>
    );
};
