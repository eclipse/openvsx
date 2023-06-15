/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.admin;

import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MonthlyAdminStatisticsJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    @Autowired
    JobRequestScheduler scheduler;

    @Override
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        var lastMonth = TimeUtil.getCurrentUTC().minusMonths(1);
        var year = lastMonth.getYear();
        var month = lastMonth.getMonthValue();

        var jobIdText = "AdminStatistics::year=" + year + ",month=" + month;
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.enqueue(jobId, new AdminStatisticsJobRequest(year, month));
    }
}
