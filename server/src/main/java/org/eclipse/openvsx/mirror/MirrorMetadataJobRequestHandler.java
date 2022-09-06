/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.Scheduler;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MirrorMetadataJobRequestHandler implements JobRequestHandler<MirrorMetadataJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorMetadataJobRequestHandler.class);
    
    @Autowired
    Scheduler scheduler;

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Override
    @Job(name="Mirror Metadata", retries=10)
    public void run(MirrorMetadataJobRequest jobRequest) throws Exception {
        if(!enabled) {
            return;
        }

        LOGGER.info(">> Starting MirrorMetadataJob");
        var now = TimeUtil.toUTCString(LocalDateTime.now());
        var extensions = repositories.findAllActiveExtensions();
        for(var extension : extensions) {
            var namespaceName = extension.getNamespace().getName();
            var extensionName = extension.getName();
            scheduler.enqueueMirrorExtensionMetadata(namespaceName, extensionName, now);
        }

        LOGGER.info("<< Completed MirrorMetadataJob");
    }
}
