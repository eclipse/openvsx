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

    private static String JOB_NAME;

    protected final Logger logger = LoggerFactory.getLogger(MigrationItemJobRequestHandler.class);

    private final RepositoryService repositories;
    private final MigrationService migrations;
    private final JobRequestScheduler scheduler;

    public MigrationItemJobRequestHandler(
            RepositoryService repositories,
            MigrationService migrations,
            JobRequestScheduler scheduler
    ) {
        this.repositories = repositories;
        this.migrations = migrations;
        this.scheduler = scheduler;
    }

    @Value("${ovsx.registry.version:}")
    public void setJobName(String version) {
        var jobIdText = "ScheduleMigrationItems::" + version;
        JOB_NAME = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String getJobName() {
        return JOB_NAME;
    }

    @Override
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        var items = repositories.findNotMigratedItems(PageRequest.ofSize(25000));
        for(var item : items) {
            migrations.enqueueMigration(item);
        }

        logger.info("Scheduled migration items: {}", items.getSize());
        if(!items.hasNext()) {
            scheduler.deleteRecurringJob(getJobName());
        }
    }
}
