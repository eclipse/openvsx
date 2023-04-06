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
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class SetPreReleaseJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtractResourcesJobRequestHandler.class));

    @Autowired
    MigrationService migrations;

    @Autowired
    SetPreReleaseJobService service;

    @Override
    @Job(name = "Set pre-release and preview for published extensions", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var extVersions = service.getExtensionVersions(jobRequest, logger);
        for(var extVersion : extVersions) {
            var entry = migrations.getDownload(extVersion);
            try (var extensionFile = migrations.getExtensionFile(entry)) {
                service.updatePreviewAndPreRelease(extVersion, extensionFile);
            }
        }
    }
}
