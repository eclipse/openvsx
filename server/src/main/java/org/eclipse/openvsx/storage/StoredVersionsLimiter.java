/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.storage;

import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StoredVersionsLimiter {

    @Autowired
    JobRequestScheduler scheduler;

    @Value("${ovsx.storage.versions-limiter.run-on-start:false}")
    boolean runOnStart;

    @Value("${ovsx.storage.versions-limiter.user-name:versions-limiter}")
    String userName;

    @Value("${ovsx.storage.versions-limiter.limit:0}")
    int limit;

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        if(!runOnStart || !isEnabled()) {
            return;
        }

        scheduler.enqueue(new LimitStoredVersionsJobRequest(true));
    }

    public boolean isEnabled() {
        return limit > 0;
    }
    public int getLimit() {
        return limit;
    }

    public String getUserName() {
        return userName;
    }
}
