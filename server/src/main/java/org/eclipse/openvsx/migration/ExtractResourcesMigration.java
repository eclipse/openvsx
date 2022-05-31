/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class ExtractResourcesMigration {

    @Autowired
    RepositoryService repositories;

    @Autowired
    JobRequestScheduler scheduler;

    @EventListener
    @Transactional
    public void enqueueJobs(ApplicationStartedEvent event) {
        repositories.findNotMigratedResources()
                .forEach(item -> {
                    var id = item.getId();
                    var jobIdText = "ExtractResourcesMigration::itemId=" + id;
                    var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));

                    scheduler.enqueue(jobId, new ExtractResourcesJobRequest(id));
                    item.setMigrationScheduled(true);
                });
    }
}
