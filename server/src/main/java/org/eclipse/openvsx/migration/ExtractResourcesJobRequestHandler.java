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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class ExtractResourcesJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtractResourcesJobRequestHandler.class));

    private final ExtractResourcesJobService service;
    private final MigrationService migrations;

    public ExtractResourcesJobRequestHandler(ExtractResourcesJobService service, MigrationService migrations) {
        this.service = service;
        this.migrations = migrations;
    }

    @Override
    @Job(name = "Extract resources from published extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var extVersion = migrations.getExtension(jobRequest.getEntityId());
        logger.info("Extracting resources for: {}", NamingUtil.toLogFormat(extVersion));

        service.deleteResources(extVersion);
        var download = migrations.getDownload(extVersion);
        if(download == null) {
            return;
        }

        try(var extensionFile = migrations.getExtensionFile(download)) {
            if(Files.size(extensionFile.getPath()) == 0) {
                return;
            }
            try (var extProcessor = new ExtensionProcessor(extensionFile)) {
                extProcessor.processEachResource(download.getExtension(), (tempFile) -> {
                    migrations.uploadFileResource(tempFile);
                    migrations.persistFileResource(tempFile.getResource());
                });
            }
        }

        service.deleteWebResources(extVersion);
    }
}
