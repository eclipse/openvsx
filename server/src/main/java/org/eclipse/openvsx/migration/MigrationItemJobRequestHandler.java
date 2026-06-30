/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.settings.SettingsService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MigrationItemJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    protected final Logger logger = LoggerFactory.getLogger(MigrationItemJobRequestHandler.class);

    private final SettingsService settings;
    private final RepositoryService repositories;
    private final MigrationService migrations;
    private final MigrationScheduler scheduler;

    public MigrationItemJobRequestHandler(
            SettingsService settings,
            RepositoryService repositories,
            MigrationService migrations,
            MigrationScheduler scheduler
    ) {
        this.settings = settings;
        this.repositories = repositories;
        this.migrations = migrations;
        this.scheduler = scheduler;
    }

    @Override
    @Job(name = "Migration item processing", retries = 0)
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        if (settings.isReadOnly()) {
            return;
        }

        var items = repositories.findNotMigratedItems(PageRequest.ofSize(25000));
        for (var item : items) {
            migrations.enqueueMigration(item);
        }

        logger.info("Scheduled migration items: {}", items.getNumberOfElements());
        if (!items.hasNext()) {
            logger.info("Migration completed, deleting recurring job");
            scheduler.deleteScheduleMigrationItemsJob();
        }
    }
}
