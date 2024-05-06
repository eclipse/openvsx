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

import io.micrometer.observation.ObservationRegistry;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.AbstractMap;

@Component
public class GenerateSha256ChecksumJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(GenerateSha256ChecksumJobRequestHandler.class));

    private final MigrationService migrations;

    public GenerateSha256ChecksumJobRequestHandler(MigrationService migrations) {
        this.migrations = migrations;
    }

    @Override
    @Job(name = "Generate sha256 checksum for published extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var download = migrations.getResource(jobRequest);
        var extVersion = download.getExtension();
        logger.info("Generate sha256 checksum for: {}", NamingUtil.toLogFormat(extVersion));

        var existingChecksum = migrations.getFileResource(extVersion, FileResource.DOWNLOAD_SHA256);
        if(existingChecksum != null) {
            migrations.removeFile(existingChecksum);
            migrations.deleteFileResource(existingChecksum);
        }

        var content = migrations.getContent(download);
        var entry = new AbstractMap.SimpleEntry<>(download, content);
        try(var extensionFile = migrations.getExtensionFile(entry)) {
            if(Files.size(extensionFile.getPath()) == 0) {
                return;
            }
            try (var extProcessor = new ExtensionProcessor(extensionFile, ObservationRegistry.NOOP)) {
                var checksum = extProcessor.generateSha256Checksum(extVersion);
                checksum.setStorageType(download.getStorageType());
                migrations.uploadFileResource(checksum);
                migrations.persistFileResource(checksum);
            }
        }
    }
}
