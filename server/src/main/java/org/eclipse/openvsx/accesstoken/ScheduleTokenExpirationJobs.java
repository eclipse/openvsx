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
package org.eclipse.openvsx.accesstoken;

import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class ScheduleTokenExpirationJobs {

    private final Logger logger = LoggerFactory.getLogger(ScheduleTokenExpirationJobs.class);

    private final AccessTokenConfig config;
    private final JobRequestScheduler scheduler;

    public ScheduleTokenExpirationJobs(AccessTokenConfig config, JobRequestScheduler scheduler) {
        this.config = config;
        this.scheduler = scheduler;
    }

    @EventListener
    public void scheduleJobs(ApplicationStartedEvent event) {
        if (config.expiration != null && config.expiration.isPositive()) {
            scheduler.enqueue(new HandlerJobRequest<>(LegacyPersonalAccessTokenExpirationHandler.class));
        }

        if (StringUtils.hasText(config.expirationSchedule)) {
            logger.info("Scheduling access token expiration job with schedule '{}'", config.expirationSchedule);
            scheduler.scheduleRecurrently(
                    "access-token-expiration",
                    config.expirationSchedule,
                    new HandlerJobRequest<>(ExpirePersonalAccessTokensHandler.class)
            );
        } else {
            scheduler.deleteRecurringJob("access-token-expiration");
        }

        if (StringUtils.hasText(config.notificationSchedule) && config.notification.isPositive()) {
            logger.info("Scheduling access token notification job with schedule '{}'", config.notificationSchedule);
            scheduler.scheduleRecurrently(
                    "access-token-notification",
                    config.notificationSchedule,
                    new HandlerJobRequest<>(NotifyPersonalAccessTokenExpirationHandler.class));
        } else {
            scheduler.deleteRecurringJob("access-token-notification");
        }
    }
}
