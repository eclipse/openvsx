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
package org.eclipse.openvsx.ratelimit;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CollectUsageStatsJobs {
    private final Logger logger = LoggerFactory.getLogger(CollectUsageStatsJobs.class);

    private CustomerUsageService customerUsageService;

    public CollectUsageStatsJobs(CustomerUsageService customerUsageService) {
        this.customerUsageService = customerUsageService;
    }

    @Job(name = "Collect usage stats", retries = 0)
    @Recurring(id = "collect-usage-stats", cron = "*/15 * * * * *", zoneId = "UTC")
    public void collect() {
        logger.info("starting collect usage stats job");

        customerUsageService.persistUsageStats();
    }
}
