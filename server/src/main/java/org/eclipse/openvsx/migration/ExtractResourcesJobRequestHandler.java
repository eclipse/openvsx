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
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExtractResourcesJobRequestHandler implements JobRequestHandler<ExtractResourcesJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtractResourcesJobRequestHandler.class));

    @Autowired
    ExtractResourcesJobService service;

    @Override
    @Job(name = "Extract resources from published extension version", retries = 3)
    public void run(ExtractResourcesJobRequest jobRequest) throws Exception {
        var extVersion = service.getExtension(jobRequest.getItemId());
        logger.info("Extracting resources for: {}.{}-{}@{}", extVersion.getExtension().getNamespace().getName(), extVersion.getExtension().getName(), extVersion.getVersion(), extVersion.getTargetPlatform());

        service.deleteResources(extVersion);
        var entry = service.getDownload(extVersion);
        var extensionFile = service.getExtensionFile(entry);
        var download = entry.getKey();
        try(var extProcessor = new ExtensionProcessor(extensionFile)) {
            extProcessor.processEachResource(download.getExtension(), (resource) -> {
                resource.setStorageType(download.getStorageType());
                service.uploadResource(resource);
                service.persistResource(resource);
            });
        }

        service.deleteWebResources(extVersion);
    }
}
