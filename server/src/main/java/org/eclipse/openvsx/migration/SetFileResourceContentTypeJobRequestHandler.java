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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class SetFileResourceContentTypeJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(SetFileResourceContentTypeJobRequestHandler.class);

    @Autowired
    MigrationService migrations;

    @Autowired
    SetFileResourceContentTypeJobService service;

    @Override
    @Job(name = "Set content type for published file resources", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var extVersion = migrations.getExtension(jobRequest.getEntityId());
        logger.info("Set content type for: {}", NamingUtil.toLogFormat(extVersion));

        var entry = migrations.getDownload(extVersion);
        try(var extensionFile = migrations.getExtensionFile(entry)) {
            try (var processor = new ExtensionProcessor(extensionFile)) {
                Consumer<FileResource> consumer = resource -> {
                    var existingResource = service.updateExistingResource(extVersion, resource);
                    if (existingResource == null) {
                        return;
                    }

                    var content = migrations.getContent(existingResource);
                    try (var resourceFile = migrations.getExtensionFile(new AbstractMap.SimpleEntry<>(existingResource, content))) {
                        migrations.removeFile(existingResource);
                        migrations.uploadFileResource(existingResource, resourceFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                processor.getFileResources(extVersion).forEach(consumer);
                processor.processEachResource(extVersion, consumer);
            }
        }
    }
}
