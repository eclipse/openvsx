/** ******************************************************************************
 * Copyright (c) 2026 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import java.nio.file.Files;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.util.NamingUtil;

/**
 * Backfills {@link org.eclipse.openvsx.entities.FileResource#getSize()} for file resources that were
 * stored before the size was tracked.
 * <p>
 * Disabled on mirror instances: mirrored resources are mostly served on the fly rather than stored
 * locally (see {@code PublishExtensionVersionHandler#mirror}), so downloading them here to measure
 * their size would just fail for anything other than the download and its sha256 checksum.
 */
@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class FileResourceSizeJobRequestHandler implements JobRequestHandler<MigrationJobRequest<?>> {

    protected final Logger logger = new JobRunrDashboardLogger(
            LoggerFactory.getLogger(FileResourceSizeJobRequestHandler.class));

    private final MigrationService migrations;

    public FileResourceSizeJobRequestHandler(MigrationService migrations) {
        this.migrations = migrations;
    }

    @Override
    @Job(name = "Determine file size for published file resource", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var resource = migrations.getResource(jobRequest);
        if (resource == null || resource.getSize() != null) {
            return;
        }

        logger.atInfo()
                .setMessage("Determine file size for: {} ({})")
                .addArgument(() -> NamingUtil.toLogFormat(resource.getExtension()))
                .addArgument(resource::getName)
                .log();

        try (var file = migrations.getExtensionFile(resource)) {
            resource.setSize(Files.size(file.getPath()));
            migrations.updateResource(resource);
        }
    }
}
