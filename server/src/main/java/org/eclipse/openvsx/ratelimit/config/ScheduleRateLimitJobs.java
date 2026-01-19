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
package org.eclipse.openvsx.ratelimit.config;

import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.eclipse.openvsx.ratelimit.jobs.CollectUsageStatsJobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScheduleRateLimitJobs {

    private static final String COLLECT_USAGE_STATS_SCHEDULE = "*/15 * * * * *";

    private UsageDataService usageDataService;
    private final JobRequestScheduler scheduler;

    public ScheduleRateLimitJobs(Optional<UsageDataService> usageDataService, JobRequestScheduler scheduler) {
        usageDataService.ifPresent(service -> this.usageDataService = service);
        this.scheduler = scheduler;
    }

    @EventListener
    public void scheduleJobs(ApplicationStartedEvent event) {
        if (usageDataService != null) {
            scheduler.scheduleRecurrently("collect-usage-stats", COLLECT_USAGE_STATS_SCHEDULE, new CollectUsageStatsJobRequest());
        } else {
            scheduler.deleteRecurringJob("collect-usage-stats");
        }
    }
}
