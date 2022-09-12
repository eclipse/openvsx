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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class MirrorExtensionMetadataJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionMetadataJob.class);

    @Autowired
    DataMirrorService data;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var namespaceName = context.getMergedJobDataMap().getString("namespace");
        var extensionName = context.getMergedJobDataMap().getString("extension");
        data.updateMetadata(namespaceName, extensionName);
        completed(context, logger);
    }
}
