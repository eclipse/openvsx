/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.migration;

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
 * stored before the size was tracked, via a metadata-only lookup against the storage backend
 * (e.g. an S3 HEAD request) rather than downloading each file's content -- with potentially millions
 * of stored resources across a large registry, downloading every one of them just to measure it would
 * be far too slow and re-transfer an enormous amount of data for no reason.
 * <p>
 * Disabled on mirror instances: mirrored resources are mostly served on the fly rather than stored
 * locally (see {@code PublishExtensionVersionHandler#mirror}), so there is usually no stored object to
 * look up metadata for in the first place, other than the download and its sha256 checksum.
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

        resource.setSize(migrations.getFileSize(resource));
        migrations.updateResource(resource);
    }
}
