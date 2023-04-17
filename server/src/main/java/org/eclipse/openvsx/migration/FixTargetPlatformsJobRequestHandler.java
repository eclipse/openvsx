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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.AbstractMap;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class FixTargetPlatformsJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(FixTargetPlatformsJobRequestHandler.class));

    @Autowired
    ExtensionService extensions;

    @Autowired
    AdminService admins;

    @Autowired
    MigrationService migrations;

    @Autowired
    FixTargetPlatformsService service;

    @Override
    @Job(name = "Fix target platform for published extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var download = migrations.getResource(jobRequest);
        var extVersion = download.getExtension();
        var content = migrations.getContent(download);
        try (var extensionFile = migrations.getExtensionFile(new AbstractMap.SimpleEntry<>(download, content))) {
            boolean fixTargetPlatform;
            try (var extProcessor = new ExtensionProcessor(extensionFile)) {
                fixTargetPlatform = !extProcessor.getMetadata().getTargetPlatform().equals(extVersion.getTargetPlatform());
            }

            if (fixTargetPlatform) {
                logger.info("Fixing target platform for: {}", NamingUtil.toLogFormat(extVersion));
                deleteExtension(extVersion);
                try (var input = Files.newInputStream(extensionFile.getPath())) {
                    extensions.publishVersion(input, extVersion.getPublishedWith());
                }
            }
        }
    }

    private void deleteExtension(ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        admins.deleteExtension(
                extension.getNamespace().getName(),
                extension.getName(),
                extVersion.getTargetPlatform(),
                extVersion.getVersion(),
                service.getUser()
        );
    }
}
