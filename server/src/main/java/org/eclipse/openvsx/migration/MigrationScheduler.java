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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class MigrationScheduler {

    private final JobRequestScheduler scheduler;

    @Value("${ovsx.migrations.delay.seconds:0}")
    long delay;

    @Value("${ovsx.registry.version:}")
    String registryVersion;

    public MigrationScheduler(JobRequestScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var instant = Instant.now().plusSeconds(delay);
        var jobIdText = "MigrationRunner::" + registryVersion;
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.schedule(jobId, instant, new HandlerJobRequest<>(MigrationRunner.class));
    }
}
