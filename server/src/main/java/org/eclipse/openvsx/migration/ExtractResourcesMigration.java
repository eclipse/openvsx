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

import org.eclipse.openvsx.mirror.MirrorSitemapJob;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.SchedulerService;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class ExtractResourcesMigration {

    protected final Logger logger = LoggerFactory.getLogger(ExtractResourcesMigration.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    SchedulerService schedulerService;

    @EventListener
    @Transactional
    public void enqueueJobs(ApplicationStartedEvent event) {
        var resources = repositories.findNotMigratedResources().toList();
        for(var i = 0; i < resources.size(); i++) {
            try {
                var resource = resources.get(i);
                schedulerService.extractResourcesMigration(resource.getId(), i);
                resource.setMigrationScheduled(true);
            } catch (SchedulerException e) {
                logger.error("Failed to schedule ExtractResourcesMigration", e);
            }
        }
    }
}
