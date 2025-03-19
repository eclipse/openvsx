/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class ScheduleMigrationsListener {

    protected final Logger logger = LoggerFactory.getLogger(ScheduleMigrationsListener.class);

    private final JobRequestScheduler scheduler;
    private final MigrationService migrations;

    @Value("${ovsx.migrations.delay.seconds:0}")
    long delay;

    @Value("${ovsx.registry.version:}")
    String registryVersion;

    public ScheduleMigrationsListener(JobRequestScheduler scheduler, MigrationService migrations) {
        this.scheduler = scheduler;
        this.migrations = migrations;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        // TODO remove after deployment of v0.23.5
        migrations.clearJobQueue();

        var instant = Instant.now().plusSeconds(delay);
        var jobIdText = "MigrationScheduler::" + registryVersion;
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.schedule(jobId, instant, new HandlerJobRequest<>(MigrationScheduler.class));
    }
}
