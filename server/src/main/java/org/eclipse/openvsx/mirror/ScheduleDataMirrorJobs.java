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

import org.eclipse.openvsx.schedule.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ScheduleDataMirrorJobs {

    @Autowired
    Scheduler scheduler;

    @Autowired
    DataMirrorService data;

    @Value("${ovsx.data.mirror.schedule.extensions:}")
    String extensionsSchedule;

    @Value("${ovsx.data.mirror.schedule.metadata:}")
    String metadataSchedule;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Value("${ovsx.data.mirror.user-name:}")
    String userName;

    @EventListener
    public void scheduleJobs(ApplicationStartedEvent event) {
        if(enabled) {
            data.createMirrorUser(userName);
            scheduler.scheduleMirrorExtensions(extensionsSchedule);
            scheduler.scheduleMirrorMetadata(metadataSchedule);
        } else {
            scheduler.deleteMirrorExtensions();
            scheduler.deleteMirrorMetadata();
        }
    }
}
