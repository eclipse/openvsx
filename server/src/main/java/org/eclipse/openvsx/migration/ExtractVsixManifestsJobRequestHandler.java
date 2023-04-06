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
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class ExtractVsixManifestsJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtractVsixManifestsJobRequestHandler.class));

    @Autowired
    MigrationService migrations;

    @Override
    @Job(name = "Extract VSIX manifests from published extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var download = migrations.getResource(jobRequest);
        var extVersion = download.getExtension();
        logger.info("Extracting VSIX manifests for: {}", NamingUtil.toLogFormat(extVersion));

        var existingVsixManifest = migrations.getFileResource(extVersion, FileResource.VSIXMANIFEST);
        if(existingVsixManifest != null) {
            migrations.removeFile(existingVsixManifest);
            migrations.deleteFileResource(existingVsixManifest);
        }

        var content = migrations.getContent(download);
        try(
                var extensionFile = migrations.getExtensionFile(new AbstractMap.SimpleEntry<>(download, content));
                var extProcessor = new ExtensionProcessor(extensionFile)
        ) {
            var vsixManifest = extProcessor.getVsixManifest(extVersion);
            vsixManifest.setStorageType(download.getStorageType());
            migrations.uploadFileResource(vsixManifest);
            migrations.persistFileResource(vsixManifest);
        }
    }
}
