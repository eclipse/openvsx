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

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MigrationScheduler implements JobRequestHandler<HandlerJobRequest<?>> {

    private final static String SCHEDULE_MIGRATION_ITEMS_JOB = "schedule-migration-items";

    private final OrphanNamespaceMigration orphanNamespaceMigration;
    private final JobRequestScheduler scheduler;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorEnabled;

    public MigrationScheduler(
            OrphanNamespaceMigration orphanNamespaceMigration,
            JobRequestScheduler scheduler
    ) {
        this.orphanNamespaceMigration = orphanNamespaceMigration;
        this.scheduler = scheduler;
    }

    @Override
    @Job(name = "Schedule migrations", retries = 0)
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        orphanNamespaceMigration.fixOrphanNamespaces();
        if(!mirrorEnabled) {
            scheduler.enqueue(new HandlerJobRequest<>(GenerateKeyPairJobRequestHandler.class));
        }

        scheduler.scheduleRecurrently(
                SCHEDULE_MIGRATION_ITEMS_JOB,
                Cron.every15minutes(),
                new HandlerJobRequest<>(MigrationItemJobRequestHandler.class)
        );
    }

    public void deleteScheduleMigrationItemsJob() {
        scheduler.deleteRecurringJob(SCHEDULE_MIGRATION_ITEMS_JOB);
    }
}
