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

import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.states.IllegalJobStateChangeException;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class ScheduleMigrationsListener {

    protected final Logger logger = LoggerFactory.getLogger(ScheduleMigrationsListener.class);

    private final JobRequestScheduler scheduler;
    private final RepositoryService repositories;

    @Value("${ovsx.migrations.delay.seconds:0}")
    long delay;

    @Value("${ovsx.registry.version:}")
    String registryVersion;

    public ScheduleMigrationsListener(JobRequestScheduler scheduler, RepositoryService repositories) {
        this.scheduler = scheduler;
        this.repositories = repositories;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        // TODO remove after deployment of v0.23.1
        Pageable page = PageRequest.ofSize(10000);
        while(page != null) {
            var migrationItems = repositories.findMigrationItemsByJobName("RemoveFileResourceTypeResourceMigration", page);
            for (var item : migrationItems) {
                var jobIdText = item.getJobName() + "::itemId=" + item.getId();
                var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
                try {
                    scheduler.delete(jobId);
                } catch (JobNotFoundException | IllegalJobStateChangeException | ConcurrentJobModificationException e) {
                    if(!(e instanceof JobNotFoundException)) {
                        logger.warn("Failed to delete job", e);
                    }
                }
            }

            page = migrationItems.hasNext() ? migrationItems.getPageable().next() : null;
        }

        var instant = Instant.now().plusSeconds(delay);
        var jobIdText = "MigrationScheduler::" + registryVersion;
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.schedule(jobId, instant, new HandlerJobRequest<>(MigrationScheduler.class));
    }
}
