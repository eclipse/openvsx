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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MigrationScheduler {

    @Autowired
    JobRequestScheduler scheduler;

    @Value("${ovsx.migrations.delay.seconds:0}")
    long delay;

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var instant = Instant.now().plusSeconds(delay);
        scheduler.schedule(instant, new HandlerJobRequest<>(MigrationRunner.class));
    }
}
