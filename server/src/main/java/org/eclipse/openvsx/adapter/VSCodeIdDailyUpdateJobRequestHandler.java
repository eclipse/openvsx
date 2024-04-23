/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class VSCodeIdDailyUpdateJobRequestHandler implements JobRequestHandler<JobRequest> {

    private final VSCodeIdUpdateService service;

    public VSCodeIdDailyUpdateJobRequestHandler(VSCodeIdUpdateService service) {
        this.service = service;
    }

    @Override
    public void run(JobRequest request) throws Exception {
        service.updateAll();
    }
}
