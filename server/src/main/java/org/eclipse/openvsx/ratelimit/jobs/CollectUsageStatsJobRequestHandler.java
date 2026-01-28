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
package org.eclipse.openvsx.ratelimit.jobs;

import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CollectUsageStatsJobRequestHandler implements JobRequestHandler<CollectUsageStatsJobRequest> {
    private final Logger logger = LoggerFactory.getLogger(CollectUsageStatsJobRequestHandler.class);

    private UsageDataService usageDataService;

    public CollectUsageStatsJobRequestHandler(Optional<UsageDataService> usageDataService) {
        usageDataService.ifPresent(service -> this.usageDataService = service);
    }

    @Override
    @Job(name = "Collect usage stats")
    public void run(CollectUsageStatsJobRequest collectUsageStatsJobRequest) throws Exception {
        if (usageDataService != null) {
            logger.info("tiered-rate-limit: starting collect usage stats job");
            usageDataService.persistUsageStats();
        }
    }
}
