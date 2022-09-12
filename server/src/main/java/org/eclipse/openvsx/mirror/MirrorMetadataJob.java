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
import org.eclipse.openvsx.schedule.SchedulerService;
import org.eclipse.openvsx.util.TimeUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class MirrorMetadataJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorMetadataJob.class);

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    RepositoryService repositories;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var now = TimeUtil.toUTCString(LocalDateTime.now());
        var extensions = repositories.findAllActiveExtensions().toList();
        for(var i = 0; i < extensions.size(); i++) {
            var extension = extensions.get(i);
            var namespaceName = extension.getNamespace().getName();
            var extensionName = extension.getName();
            try {
                schedulerService.mirrorExtensionMetadata(namespaceName, extensionName, now, i);
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
        }

        completed(context, logger);
    }
}
