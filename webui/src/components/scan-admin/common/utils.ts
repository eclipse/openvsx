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

// ============================================================================
// Date/Time Formatting Utilities
// ============================================================================

export const formatDuration = (start: string, end?: string): string => {
    if (!end) return 'Scan in progress...';
    const duration = new Date(end).getTime() - new Date(start).getTime();
    if (duration < 1000) return '< 1 sec';
    const seconds = Math.floor(duration / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    if (minutes === 0) {
        return `${remainingSeconds} sec`;
    }
    return `${minutes} min ${remainingSeconds} sec`;
};

export const formatDateTime = (isoString: string): string => {
    const date = new Date(isoString);
    const pad = (n: number) => n < 10 ? '0' + n : n.toString();

    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    let hours = date.getHours();
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12;
    const formattedHours = pad(hours);
    const timeZone = date.toLocaleTimeString('en-US', { timeZoneName: 'short' }).split(' ')[2];

    return `${year}-${month}-${day} ${formattedHours}:${minutes}:${seconds} ${ampm} ${timeZone}`;
};
