/******************************************************************************
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
 *****************************************************************************/

import { useContext, useState, useEffect, useRef, useCallback } from "react";
import { MainContext } from "../../../context";
import type { UsageStats } from "../../../extension-registry-types";
import { handleError } from "../../../utils";
import { getDefaultStartDate } from "./usage-stats-utils";
import { DateTime } from "luxon";

export const useUsageStats = (customerName: string | undefined) => {
    const abortController = useRef(new AbortController());
    const { service } = useContext(MainContext);

    const [usageStats, setUsageStats] = useState<readonly UsageStats[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [startDate, setStartDate] = useState<DateTime>(getDefaultStartDate);

    const loadUsageStats = useCallback(async () => {
        if (!customerName) {
            setUsageStats([]);
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            setError(null);
            const data = await service.admin.getUsageStats(
                abortController.current,
                customerName,
                startDate.toJSDate()
            );
            setUsageStats(data.stats);
        } catch (err) {
            setError(handleError(err as Error));
        } finally {
            setLoading(false);
        }
    }, [service, customerName, startDate]);

    useEffect(() => {
        if (customerName) {
            loadUsageStats();
        }
    }, [loadUsageStats, customerName]);

    useEffect(() => {
        return () => abortController.current.abort();
    }, []);

    return { usageStats, loading, error, startDate, setStartDate };
};
