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

import React, { useRef, useEffect, useState, useLayoutEffect } from 'react';
import { Box, Chip, Typography } from '@mui/material';
import { useTheme, alpha } from '@mui/material/styles';
import { DetailBadge } from './utils';

interface ScanCardExpandStripBadgesProps {
    badges: DetailBadge[];
    containerWidth: number;
    maxWidthPercent?: number;
}

/**
 * Renders badges with overflow handling - shows as many badges as fit
 * within the available width, plus a "+ X more" indicator for hidden badges.
 */
export const ScanCardExpandStripBadges: React.FC<ScanCardExpandStripBadgesProps> = ({
    badges,
    containerWidth,
    maxWidthPercent = 0.45,
}) => {
    const theme = useTheme();
    const measureRef = useRef<HTMLDivElement>(null);
    const [visibleCount, setVisibleCount] = useState<number | null>(null);

    const calculateVisibleBadges = () => {
        if (!measureRef.current || badges.length === 0 || containerWidth <= 0) {
            return;
        }

        const availableWidth = containerWidth * maxWidthPercent;
        const chips = measureRef.current.querySelectorAll('.measure-chip');

        if (chips.length === 0) {
            return;
        }

        // 65px width for "+ X more" text
        const moreIndicatorWidth = 65;
        const gap = 4; // 0.5 spacing unit = 4px

        let totalWidth = 0;
        let count = 0;

        for (let index = 0; index < chips.length; index++) {
            const chip = chips[index] as HTMLElement;
            const chipWidth = chip.offsetWidth;
            const widthWithGap = chipWidth + (index > 0 ? gap : 0);

            // Check if this chip fits
            // If not all badges fit, we need room for the "+ X more" indicator
            const remainingBadges = badges.length - (index + 1);
            const needsMoreIndicator = remainingBadges > 0;
            const reservedWidth = needsMoreIndicator ? moreIndicatorWidth + gap : 0;

            if (totalWidth + widthWithGap + reservedWidth <= availableWidth) {
                totalWidth += widthWithGap;
                count++;
            } else {
                break;
            }
        }

        setVisibleCount(count);
    };

    // Use layoutEffect to measure before paint (to avoid flicker)
    useLayoutEffect(() => {
        calculateVisibleBadges();
    }, [badges, containerWidth]);

    // Recalculate when container width changes
    useEffect(() => {
        calculateVisibleBadges();
    }, [containerWidth]);

    // Use all badges until calculation completes
    const effectiveVisibleCount = visibleCount ?? badges.length;
    const visibleBadges = badges.slice(0, effectiveVisibleCount);
    const hiddenCount = badges.length - effectiveVisibleCount;

    const chipStyles = (badge: DetailBadge) => ({
        height: '20px',
        fontSize: '0.7rem',
        flexShrink: 0,
        ...(badge.type === 'threat' ? {
            backgroundColor: badge.isEnforced ? theme.palette.quarantined.dark : undefined,
            color: theme.palette.quarantined.light,
            background: !badge.isEnforced ? `${theme.palette.unenforced.stripe}, ${theme.palette.quarantined.dark}` : undefined,
        } : {
            backgroundColor: badge.isEnforced ? theme.palette.rejected.dark : undefined,
            color: theme.palette.rejected.light,
            background: !badge.isEnforced ? `${theme.palette.unenforced.stripe}, ${theme.palette.rejected.dark}` : undefined,
        }),
    });

    if (badges.length === 0) {
        return null;
    }

    return (
        <>
            {/* Hidden measurement container - always renders all badges for measurement */}
            <Box
                ref={measureRef}
                sx={{
                    position: 'absolute',
                    visibility: 'hidden',
                    pointerEvents: 'none',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    flexWrap: 'nowrap',
                    left: 0,
                    top: 0,
                }}
                aria-hidden='true'
            >
                {badges.map((badge, index) => (
                    <Chip
                        key={`measure-${badge.label}-${index}`}
                        className='measure-chip'
                        label={badge.label}
                        size='small'
                        sx={chipStyles(badge)}
                    />
                ))}
            </Box>

            {/* Visible container - only shows badges that fit */}
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    flexWrap: 'nowrap',
                    overflow: 'hidden',
                    maxWidth: `${maxWidthPercent * 100}%`,
                    minWidth: 0,
                }}
            >
                {visibleBadges.map((badge, index) => (
                    <Chip
                        key={`${badge.label}-${index}`}
                        label={badge.label}
                        size='small'
                        sx={chipStyles(badge)}
                    />
                ))}
                {hiddenCount > 0 && (
                    <Typography
                        variant='caption'
                        sx={{
                            flexShrink: 0,
                            color: alpha(theme.palette.text.secondary, 0.8),
                            fontSize: '0.7rem',
                            fontWeight: 500,
                            whiteSpace: 'nowrap',
                            ml: 0.5,
                        }}
                    >
                        + {hiddenCount} more
                    </Typography>
                )}
            </Box>
        </>
    );
};
