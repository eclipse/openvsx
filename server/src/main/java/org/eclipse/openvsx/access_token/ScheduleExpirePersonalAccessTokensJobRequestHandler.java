/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.access_token;

import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class ScheduleExpirePersonalAccessTokensJobRequestHandler implements JobRequestHandler<HandlerJobRequest> {

    private final JobRequestScheduler scheduler;

    public ScheduleExpirePersonalAccessTokensJobRequestHandler(JobRequestScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run(HandlerJobRequest handlerJobRequest) throws Exception {
        var zone = ZoneId.of("UTC");
        var expireSchedule = Cron.daily(1, 38);
        var expireJobRequest = new HandlerJobRequest<>(ExpirePersonalAccessTokensJobRequestHandler.class);
        scheduler.scheduleRecurrently("ExpirePersonalAccessTokens", expireSchedule, zone, expireJobRequest);
    }
}
