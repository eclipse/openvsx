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

import { DateRangeType } from './scan-types';

/**
 * Calculates date range boundaries based on the date range type
 * @returns { from, to } ISO date strings, or { from: undefined, to: undefined } for 'all'
 */
const calculateDateRange = (dateRange: DateRangeType): { from: string | undefined; to: string | undefined } => {
    if (dateRange === 'all') {
        return { from: undefined, to: undefined };
    }

    const now = new Date();
    const to = now.toISOString();
    let from: string;

    switch (dateRange) {
        case 'today':
            from = new Date(now.getFullYear(), now.getMonth(), now.getDate()).toISOString();
            break;
        case 'last7days':
            from = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
            break;
        case 'last30days':
            from = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
            break;
        case 'last90days':
            from = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
            break;
        default:
            from = new Date(0).toISOString();
    }

    return { from, to };
};

/**
 * Converts a date range string to API date parameters for scans
 */
export const getDateRangeParams = (dateRange: DateRangeType) => {
    const { from, to } = calculateDateRange(dateRange);
    return { dateStartedFrom: from, dateStartedTo: to };
};

/**
 * Converts a date range string to API date parameters for file decisions
 */
export const getFileDateRange = (dateRange: DateRangeType) => {
    const { from, to } = calculateDateRange(dateRange);
    return { dateDecidedFrom: from, dateDecidedTo: to };
};
