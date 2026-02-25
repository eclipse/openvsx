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

import type { RefObject } from "react";
import { useState, useEffect, useRef, useMemo } from 'react';
import { ScanResult } from '../../context/scan-admin';
import { formatDuration } from '../../components/scan-admin/common';
import {
    isRunning,
    shouldShowExpandButton,
    getDetailBadges,
    DetailBadge,
} from '../../components/scan-admin';

interface UseScanCardStateReturn {
    expanded: boolean;
    handleExpandClick: () => void;
    showExpandButton: boolean;
    badges: DetailBadge[];
    liveDuration: string;
    // Card ref for height tracking
    cardRef: RefObject<HTMLDivElement>;
}

/**
 * Custom hook that encapsulates all ScanCard state logic.
 */
export const useScanCardState = (scan: ScanResult): UseScanCardStateReturn => {
    const [expanded, setExpanded] = useState(false);
    const [liveDuration, setLiveDuration] = useState('');

    const cardRef = useRef<HTMLDivElement>(null);

    const badges = useMemo(() => getDetailBadges(scan), [scan]);

    const showExpandButton = shouldShowExpandButton(scan);

    const handleExpandClick = () => {
        setExpanded(!expanded);
    };

    // Live duration for running scans
    useEffect(() => {
        if (isRunning(scan.status) && scan.dateScanStarted) {
            const updateDuration = () => {
                const duration = formatDuration(scan.dateScanStarted, new Date().toISOString());
                setLiveDuration(duration);
            };
            updateDuration();
            const interval = setInterval(updateDuration, 1000);
            return () => clearInterval(interval);
        }
        return undefined;
    }, [scan.status, scan.dateScanStarted]);

    return {
        expanded,
        handleExpandClick,
        showExpandButton,
        badges,
        liveDuration,
        cardRef,
    };
};
