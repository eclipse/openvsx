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
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class SchedulePersonalAccessTokenJobsListener {

    private final JobRequestScheduler scheduler;

    @Value("${ovsx.access-token-expire.delay:0}")
    int delay;

    public SchedulePersonalAccessTokenJobsListener(JobRequestScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var jobIdText = "ScheduleExpirePersonalAccessTokens";
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        var scheduleExpireJobRequest = new HandlerJobRequest<>(ScheduleExpirePersonalAccessTokensJobRequestHandler.class);
        if(delay > 0) {
            scheduler.schedule(jobId, TimeUtil.getCurrentUTC().plusDays(delay), scheduleExpireJobRequest);
        } else {
            scheduler.enqueue(jobId, scheduleExpireJobRequest);
        }

        var zone = ZoneId.of("UTC");
        var notifySchedule = Cron.daily(2, 8);
        var notifyJobRequest = new HandlerJobRequest<>(NotifyPersonalAccessTokenExpiryJobRequestHandler.class);
        scheduler.scheduleRecurrently("NotifyPersonalAccessTokenExpiry", notifySchedule, zone, notifyJobRequest);
    }

}
