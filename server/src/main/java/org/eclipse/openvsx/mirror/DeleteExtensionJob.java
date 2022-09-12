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

import org.eclipse.openvsx.AdminService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;

public class DeleteExtensionJob implements Job {

    protected final Logger logger = LoggerFactory.getLogger(DeleteExtensionJob.class);

    @Autowired
    AdminService admin;

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.data.mirror.user-name}")
    String userName;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var namespaceName = context.getMergedJobDataMap().getString("namespace");
        var extensionName = context.getMergedJobDataMap().getString("extension");
        var mirrorUser = repositories.findUserByLoginName(null, userName);
        admin.deleteExtension(namespaceName, extensionName, mirrorUser);
        completed(context, logger);
    }
}
