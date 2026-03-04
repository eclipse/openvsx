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

import { FunctionComponent, PropsWithChildren, useState } from 'react';
import { Box, Typography, Link, IconButton, Tooltip } from '@mui/material';
import { styled, useTheme } from '@mui/material/styles';
import {
    Check as CheckIcon,
    Warning as WarningAmberIcon,
} from '@mui/icons-material';
import { ScanResult } from '../../../context/scan-admin';
import { ConditionalTooltip, formatDateTime, formatDuration } from '../common';
import { isRunning, hasDownload, getFileName } from './utils';

/**
 * Grid cell positioned by row/column within the parent CSS Grid.
 * Note: MUI's Grid/Grid2 components are flexbox-based, not CSS Grid.
 */
const GridCell = styled(Box, {
    shouldForwardProp: (prop) => prop !== 'row' && prop !== 'column' && prop !== 'columnSpan',
})<{ row: number; column: number; columnSpan?: number }>(({ row, column, columnSpan }) => ({
    gridRow: String(row),
    gridColumn: columnSpan ? `${column} / span ${columnSpan}` : String(column),
    minWidth: 0,
}));

/** Typography with text-overflow ellipsis */
const EllipsisText = styled(Typography)({
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
});

/** Box with text-overflow ellipsis */
const EllipsisBox = styled(Box)({
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
});

/** Link with text-overflow ellipsis */
const EllipsisLink = styled(Link)({
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
});

/** Shimmer-animated text for in-progress scan states */
const ShimmerText = styled(EllipsisText)(({ theme }) => ({
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
}));

/** Circular checkbox outline with checked/hover state transitions */
const CheckboxOutline = styled(Box, {
    shouldForwardProp: (prop) => prop !== 'isChecked' && prop !== 'isHovering',
})<{ isChecked: boolean; isHovering: boolean }>(({ theme, isChecked, isHovering }) => {
    const borderColor = isHovering ? theme.palette.selected.border : theme.palette.scanBackground.light;
    const uncheckedBg = isHovering ? theme.palette.selected.background : 'transparent';

    return {
        position: 'absolute',
        width: 36,
        height: 36,
        borderRadius: '50%',
        border: isChecked ? 'none' : `2px solid ${borderColor}`,
        backgroundColor: isChecked ? theme.palette.secondary.main : uncheckedBg,
        transition: 'border-color 0.2s, background-color 0.2s',
    };
});

/** Section caption label (e.g. "Publisher", "Version") */
const CaptionLabel: FunctionComponent<PropsWithChildren> = ({ children }) => (
    <EllipsisText variant='caption' color='text.secondary'>
        {children}
    </EllipsisText>
);

/** N/A placeholder for unavailable data */
const NotAvailable: FunctionComponent = () => (
    <Typography variant='body2' color='text.disabled'>N/A</Typography>
);

/** Publisher name with external link */
const PublisherCell: FunctionComponent<{ publisher: string; publisherUrl: string | null }> = ({
    publisher,
    publisherUrl,
}) => (
    <>
        <CaptionLabel>Publisher</CaptionLabel>
        <ConditionalTooltip title={publisher} arrow>
            <EllipsisBox>
                <EllipsisLink
                    href={publisherUrl || undefined}
                    target='_blank'
                    rel='noopener noreferrer'
                    variant='body2'
                >
                    {publisher}
                </EllipsisLink>
            </EllipsisBox>
        </ConditionalTooltip>
    </>
);

/** Extension version display */
const VersionCell: FunctionComponent<{ version: string }> = ({ version }) => (
    <>
        <CaptionLabel>Version</CaptionLabel>
        <ConditionalTooltip title={version} arrow>
            <EllipsisText variant='body2'>{version}</EllipsisText>
        </ConditionalTooltip>
    </>
);

/** Target platform display */
const PlatformCell: FunctionComponent<{ targetPlatform: string }> = ({ targetPlatform }) => (
    <>
        <CaptionLabel>Platform</CaptionLabel>
        <ConditionalTooltip title={targetPlatform} arrow>
            <EllipsisText variant='body2'>{targetPlatform}</EllipsisText>
        </ConditionalTooltip>
    </>
);

/** Download link with optional quarantine warning icon */
const DownloadCell: FunctionComponent<{ scan: ScanResult }> = ({ scan }) => {
    const theme = useTheme();

    if (isRunning(scan.status)) {
        return (
            <>
                <CaptionLabel>Download</CaptionLabel>
                <NotAvailable />
            </>
        );
    }

    if (hasDownload(scan) && scan.downloadUrl) {
        return (
            <>
                <CaptionLabel>Download</CaptionLabel>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0 }}>
                    {scan.status === 'QUARANTINED' && (
                        <Tooltip
                            title='Potentially malicious'
                            arrow
                            disableInteractive
                            PopperProps={{ disablePortal: true, sx: { pointerEvents: 'none' } }}
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
                        <EllipsisLink href={scan.downloadUrl} variant='body2' sx={{ fontSize: '0.875rem', minWidth: 0 }}>
                            {getFileName(scan.downloadUrl)}
                        </EllipsisLink>
                    </ConditionalTooltip>
                </Box>
            </>
        );
    }

    return (
        <>
            <CaptionLabel>Download</CaptionLabel>
            <NotAvailable />
        </>
    );
};

/** Circular selection checkbox with hover/check animations */
const SelectionCheckbox: FunctionComponent<{
    checked?: boolean;
    onChange?: (checked: boolean) => void;
}> = ({ checked = false, onChange }) => {
    const theme = useTheme();
    const [isHovering, setIsHovering] = useState(false);
    const uncheckedIconColor = isHovering ? theme.palette.selected.border : theme.palette.scanBackground.light;

    return (
        <IconButton
            onClick={() => onChange?.(!checked)}
            onMouseEnter={() => setIsHovering(true)}
            onMouseLeave={() => setIsHovering(false)}
            disableRipple
            sx={{ padding: 0, width: 36, height: 36, backgroundColor: 'transparent' }}
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
                <CheckboxOutline
                    className='checkbox-circle-outline'
                    isChecked={checked}
                    isHovering={isHovering}
                />
                <CheckIcon
                    className='checkbox-icon'
                    sx={{
                        fontSize: 24,
                        color: checked ? 'white' : uncheckedIconColor,
                        position: 'relative',
                        zIndex: 1,
                        transition: 'color 0.2s',
                    }}
                />
            </Box>
        </IconButton>
    );
};

/** Scan start timestamp */
const ScanStartCell: FunctionComponent<{ dateScanStarted: string }> = ({ dateScanStarted }) => (
    <>
        <CaptionLabel>Scan Start</CaptionLabel>
        <ConditionalTooltip title={formatDateTime(dateScanStarted)} arrow>
            <EllipsisText variant='body2' sx={{ fontSize: '0.8rem' }}>
                {formatDateTime(dateScanStarted)}
            </EllipsisText>
        </ConditionalTooltip>
    </>
);

/** Scan end timestamp with shimmer animation for running scans */
const ScanEndCell: FunctionComponent<{ status: ScanResult['status']; dateScanEnded: string | null }> = ({
    status,
    dateScanEnded,
}) => {
    const renderValue = () => {
        if (isRunning(status)) {
            return (
                <ConditionalTooltip title={`${status}...`} arrow>
                    <ShimmerText variant='body2'>{status}...</ShimmerText>
                </ConditionalTooltip>
            );
        }
        if (dateScanEnded) {
            return (
                <ConditionalTooltip title={formatDateTime(dateScanEnded)} arrow>
                    <EllipsisText variant='body2' sx={{ fontSize: '0.8rem' }}>
                        {formatDateTime(dateScanEnded)}
                    </EllipsisText>
                </ConditionalTooltip>
            );
        }
        return <NotAvailable />;
    };

    return (
        <>
            <CaptionLabel>Scan End</CaptionLabel>
            {renderValue()}
        </>
    );
};

/** Scan duration with live counter for running scans */
const ScanDurationCell: FunctionComponent<{
    status: ScanResult['status'];
    dateScanStarted: string;
    dateScanEnded: string | null;
    liveDuration: string;
}> = ({ status, dateScanStarted, dateScanEnded, liveDuration }) => (
    <>
        <CaptionLabel>Scan Duration</CaptionLabel>
        {isRunning(status) ? (
            <ConditionalTooltip title={liveDuration} arrow>
                <ShimmerText variant='body2'>{liveDuration}</ShimmerText>
            </ConditionalTooltip>
        ) : (
            <ConditionalTooltip title={formatDuration(dateScanStarted, dateScanEnded || undefined)} arrow>
                <EllipsisText variant='body2'>
                    {formatDuration(dateScanStarted, dateScanEnded || undefined)}
                </EllipsisText>
            </ConditionalTooltip>
        )}
    </>
);

/** Admin decision status for quarantined extensions */
const DecisionStatusCell: FunctionComponent<{ adminDecision: ScanResult['adminDecision'] }> = ({ adminDecision }) => {
    const theme = useTheme();

    if (adminDecision) {
        const isAllowed = adminDecision.decision.toLowerCase() === 'allowed';
        return (
            <Tooltip
                title={`Decided by ${adminDecision.decidedBy} on ${formatDateTime(adminDecision.dateDecided)}`}
                arrow
                disableInteractive
                PopperProps={{ disablePortal: true, sx: { pointerEvents: 'none' } }}
            >
                <Typography
                    variant='h6'
                    sx={{
                        fontWeight: 700,
                        color: isAllowed ? theme.palette.allowed : theme.palette.blocked,
                        whiteSpace: 'nowrap',
                        cursor: 'help',
                    }}
                >
                    {isAllowed ? 'ALLOWED' : 'BLOCKED'}
                </Typography>
            </Tooltip>
        );
    }

    return (
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
    );
};

interface ScanCardContentProps {
    scan: ScanResult;
    showCheckbox?: boolean;
    checked?: boolean;
    onCheckboxChange?: (id: string, checked: boolean) => void;
    liveDuration: string;
}

/**
 * Content section of the ScanCard containing:
 * - Publisher, Version, Platform, Download (Row 2)
 * - Scan Start, Scan End, Duration, Decision Status (Row 3)
 * - Checkbox for selection
 */
export const ScanCardContent: FunctionComponent<ScanCardContentProps> = ({
    scan,
    showCheckbox,
    checked,
    onCheckboxChange,
    liveDuration,
}) => (
    <>
        {/* ROW 2 - Publisher, Version, Platform, Download, Checkbox */}
        <GridCell row={2} column={2}>
            <PublisherCell publisher={scan.publisher} publisherUrl={scan.publisherUrl} />
        </GridCell>
        <GridCell row={2} column={3}>
            <VersionCell version={scan.version} />
        </GridCell>
        <GridCell row={2} column={4}>
            <PlatformCell targetPlatform={scan.targetPlatform} />
        </GridCell>
        <GridCell row={2} column={5}>
            <DownloadCell scan={scan} />
        </GridCell>
        <GridCell
            row={2}
            column={6}
            sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}
        >
            {showCheckbox && (
                <SelectionCheckbox
                    checked={checked}
                    onChange={(newChecked) => onCheckboxChange?.(scan.id, newChecked)}
                />
            )}
        </GridCell>

        {/* ROW 3 - Scan Start, Scan End, Duration, Decision Status */}
        <GridCell row={3} column={2}>
            <ScanStartCell dateScanStarted={scan.dateScanStarted} />
        </GridCell>
        <GridCell row={3} column={3}>
            <ScanEndCell status={scan.status} dateScanEnded={scan.dateScanEnded} />
        </GridCell>
        <GridCell row={3} column={4}>
            <ScanDurationCell
                status={scan.status}
                dateScanStarted={scan.dateScanStarted}
                dateScanEnded={scan.dateScanEnded}
                liveDuration={liveDuration}
            />
        </GridCell>
        {scan.status === 'QUARANTINED' && (
            <GridCell
                row={3}
                column={5} columnSpan={2}
                sx={{ display: 'flex', justifyContent: 'flex-end', alignSelf: 'end' }}
            >
                <DecisionStatusCell adminDecision={scan.adminDecision} />
            </GridCell>
        )}
    </>
);
