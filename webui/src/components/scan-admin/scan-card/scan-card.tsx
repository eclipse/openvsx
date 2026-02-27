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

import { FunctionComponent, useState, useEffect } from 'react';
import { Card, CardContent, Box } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { ScanResult } from '../../../context/scan-admin';

import { ScanCardHeader } from './scan-card-header';
import { ScanCardContent } from './scan-card-content';
import { ScanCardExpandStrip } from './scan-card-expand-strip';
import { ScanCardExpandedContent } from './scan-card-expanded-content';
import { useScanCardState } from '../../../hooks/scan-admin';
import {
    ICON_SIZE,
    shouldShowStriped,
    getStatusBarColor,
} from './utils';

interface ScanCardProps {
    scan: ScanResult;
    showCheckbox?: boolean;
    onCheckboxChange?: (id: string, checked: boolean) => void;
    checked?: boolean;
}

/**
 * ScanCard component displays information about an extension scan.
 *
 * Sub-components:
 * - ScanCardHeader: Icon, display name, namespace, status badge
 * - ScanCardContent: Publisher, version, download, scan times, checkbox
 * - ScanCardExpandStrip: Collapsible trigger bar with badges
 * - ScanCardExpandedContent: Threats and validation failures
 *
 * State is managed via the useScanCardState hook.
 */
export const ScanCard: FunctionComponent<ScanCardProps> = ({
    scan,
    showCheckbox,
    onCheckboxChange,
    checked,
}) => {
    const theme = useTheme();
    const [collapseComplete, setCollapseComplete] = useState(true);
    const {
        expanded,
        handleExpandClick,
        showExpandButton,
        badges,
        liveDuration,
        cardRef,
    } = useScanCardState(scan);

    // Reset collapseComplete when expanding
    useEffect(() => {
        if (expanded) {
            setCollapseComplete(false);
        }
    }, [expanded]);

    return (
        <Card
            ref={cardRef}
            sx={{
                position: 'relative',
                mb: 1.5,
                borderRadius: 2,
                boxShadow: theme.palette.mode === 'light' ? 4 : 2,
                backgroundColor: 'transparent',
                outline: checked ? '2px solid' : 'none',
                outlineColor: checked ? 'secondary.main' : 'transparent',
                '&:hover': {
                    boxShadow: theme.palette.mode === 'light' ? 6 : 4,
                },
                overflow: 'hidden',
                paddingLeft: '16px',
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    left: 0,
                    top: 0,
                    bottom: 0,
                    width: '16px',
                    background: shouldShowStriped(scan)
                        ? `${theme.palette.unenforced.stripe}, ${getStatusBarColor(scan.status, theme)}`
                        : getStatusBarColor(scan.status, theme),
                    zIndex: 0,
                },
            }}
        >
            <CardContent
                sx={{
                    position: 'relative',
                    zIndex: 1,
                    pt: 5,
                    pr: 5,
                    pl: 5,
                    pb: showExpandButton ? 0 : 5,
                    '&:last-child': { pb: showExpandButton ? 0 : 5 },
                }}
            >
                {/* 3 Row x 6 Column Grid Layout */}
                <Box sx={{
                    display: 'grid',
                    gridTemplateColumns: `${ICON_SIZE}px 1fr 1fr 1fr 1fr 180px`,
                    gridTemplateRows: 'auto auto auto',
                    gap: 2,
                    alignItems: 'start',
                }}>
                    <ScanCardHeader scan={scan} />
                    <ScanCardContent
                        scan={scan}
                        showCheckbox={showCheckbox}
                        checked={checked}
                        onCheckboxChange={onCheckboxChange}
                        liveDuration={liveDuration}
                    />
                </Box>
            </CardContent>

            {/* Expandable strip with badges */}
            {showExpandButton && (
                <ScanCardExpandStrip
                    expanded={expanded}
                    onExpandClick={handleExpandClick}
                    badges={badges}
                    collapseComplete={collapseComplete}
                />
            )}

            {/* Expanded content */}
            {showExpandButton && (
                <ScanCardExpandedContent
                    scan={scan}
                    expanded={expanded}
                    onCollapseComplete={() => setCollapseComplete(true)}
                />
            )}
        </Card>
    );
};
