/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class PublishExtensionVersionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionJob.class);

    @Autowired
    PublishExtensionVersionService service;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var version = context.getMergedJobDataMap().getString("version");
        var targetPlatform = context.getMergedJobDataMap().getString("targetPlatform");
        var extensionName = context.getMergedJobDataMap().getString("extension");
        var namespaceName = context.getMergedJobDataMap().getString("namespace");
        try {
            service.publish(namespaceName, extensionName, targetPlatform, version);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        completed(context, logger);
    }
}
