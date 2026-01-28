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

import React, { useRef, useEffect, useState } from 'react';
import { Box } from '@mui/material';
import { ExpandMore as ExpandMoreIcon } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { DetailBadge } from './utils';
import { ScanCardExpandStripBadges } from './scan-card-expand-strip-badges';

interface ScanCardExpandStripProps {
    expanded: boolean;
    onExpandClick: () => void;
    badges: DetailBadge[];
    collapseComplete: boolean;
}

/**
 * Expandable strip at the bottom of the card
 * Shows validationFailure/threat badges (click to expand/show detail cards)
 */
export const ScanCardExpandStrip: React.FC<ScanCardExpandStripProps> = ({
    expanded,
    onExpandClick,
    badges,
    collapseComplete,
}) => {
    const theme = useTheme();
    const [isHovering, setIsHovering] = useState(false);
    const wrapperRef = useRef<HTMLDivElement>(null);
    const [containerWidth, setContainerWidth] = useState(0);

    useEffect(() => {
        const updateWidth = () => {
            if (wrapperRef.current) {
                setContainerWidth(wrapperRef.current.offsetWidth);
            }
        };

        updateWidth();

        const resizeObserver = new ResizeObserver(() => {
            updateWidth();
        });

        if (wrapperRef.current) {
            resizeObserver.observe(wrapperRef.current);
        }

        return () => resizeObserver.disconnect();
    }, []);

    return (
        <Box
            ref={wrapperRef}
            onClick={onExpandClick}
            onMouseEnter={() => setIsHovering(true)}
            onMouseLeave={() => setIsHovering(false)}
            sx={{
                pb: 0,
                pt: 0.5,
                px: 5,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
                borderBottomRightRadius: expanded ? 0 : 8,
                minHeight: 40,
                backgroundColor: isHovering ? theme.palette.selected.hover : 'transparent',
                transition: 'background-color 0.2s',
                position: 'relative',
            }}
        >
            <Box sx={{ flex: 1 }} />
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'absolute', left: '50%', transform: 'translateX(-50%)' }}>
                <ExpandMoreIcon
                    sx={{
                        fontSize: 36,
                        color: 'secondary.main',
                        transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
                        transition: 'transform 0.3s',
                    }}
                />
            </Box>

            {!expanded && collapseComplete && (
                <ScanCardExpandStripBadges
                    badges={badges}
                    containerWidth={containerWidth}
                />
            )}
        </Box>
    );
};