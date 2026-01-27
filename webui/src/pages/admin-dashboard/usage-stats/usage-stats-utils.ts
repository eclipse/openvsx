/*
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
 */

import { format, startOfWeek, startOfMonth } from 'date-fns';
import type { UsageStats, UsageStatsPeriod } from "../../../extension-registry-types";

/**
 * Maps each {@link UsageStatsPeriod} to a function that derives an aggregation key
 * string from a given {@link Date}. The resulting keys are used to group usage
 * statistics into daily, weekly, or monthly buckets.
 *
 * Each extractor must be deterministic for a given input date and return a
 * human-readable string that can be used as a grouping key.
 */
export const periodKeyExtractors: Record<UsageStatsPeriod, (date: Date) => string> = {
    daily: (date) => format(date, 'yyyy-MM-dd'),
    weekly: (date) => `Week of ${format(startOfWeek(date, { weekStartsOn: 1 }), 'yyyy-MM-dd')}`,
    monthly: (date) => format(startOfMonth(date), 'yyyy-MM')
};

/**
 * Aggregates usage statistics by a specified period, summing counts for each period key.
 * 
 * @param stats - An array of UsageStats objects to aggregate.
 * @param period - The period type (e.g., daily, weekly) used to extract keys from dates.
 * @returns An array of objects, each containing a 'period' string key and the aggregated 'count' number, sorted by period.
 */
export const aggregateByPeriod = (stats: UsageStats[], period: UsageStatsPeriod) => {
    const getKey = periodKeyExtractors[period];
    const aggregated = new Map<string, number>();

    for (const stat of stats) {
        const key = getKey(new Date(stat.windowStart));
        aggregated.set(key, (aggregated.get(key) || 0) + stat.count);
    }

    return Array.from(aggregated.entries())
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([period, count]) => ({ period, count }));
};

export const getDefaultStartDate = () => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date;
};