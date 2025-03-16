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

import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoveFileResourceTypeResourceJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(RemoveFileResourceTypeResourceJobRequestHandler.class));

    private final MigrationService migrations;

    public RemoveFileResourceTypeResourceJobRequestHandler(MigrationService migrations) {
        this.migrations = migrations;
    }

    @Override
    @Job(name = "Remove FileResource of type 'resource'", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var resource = migrations.getResource(jobRequest);
        if(resource == null) {
            return;
        }

        logger.info("Removing file resource: {} {}", NamingUtil.toLogFormat(resource.getExtension()), resource.getName());
        migrations.removeFile(resource);
        migrations.deleteFileResource(resource);
    }
}
