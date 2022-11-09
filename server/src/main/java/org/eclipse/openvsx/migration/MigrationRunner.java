/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.entities.MigrationItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MigrationRunner {

    @Autowired
    OrphanNamespaceMigration orphanNamespaceMigration;

    @Autowired
    RepositoryService repositories;

    @Autowired
    JobRequestScheduler scheduler;

    @EventListener
    @Transactional
    public void runMigrations(ApplicationStartedEvent event) {
        orphanNamespaceMigration.fixOrphanNamespaces();
        extractResourcesMigration();
        setPreReleaseMigration();
        renameDownloadsMigration();
    }

    private void extractResourcesMigration() {
        var jobName = "ExtractResourcesMigration";
        var handler = ExtractResourcesJobRequestHandler.class;
        repositories.findNotMigratedResources().forEach(item -> enqueueJob(jobName, handler, item));
    }

    private void setPreReleaseMigration() {
        var jobName = "SetPreReleaseMigration";
        var handler = SetPreReleaseJobRequestHandler.class;
        repositories.findNotMigratedPreReleases().forEach(item -> enqueueJob(jobName, handler, item));
    }

    private void renameDownloadsMigration() {
        var jobName = "RenameDownloadsMigration";
        var handler = RenameDownloadsJobRequestHandler.class;
        repositories.findNotMigratedRenamedDownloads().forEach(item -> enqueueJob(jobName, handler, item));
    }

    private void enqueueJob(String jobName, Class<? extends JobRequestHandler<MigrationJobRequest>> handler, MigrationItem item) {
        var jobIdText = jobName + "::itemId=" + item.getId();
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.enqueue(jobId, new MigrationJobRequest<>(handler, item.getEntityId()));
        item.setMigrationScheduled(true);
    }
}
