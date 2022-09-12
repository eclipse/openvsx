/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.schedule;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;

public class RetryFailedJobListener extends JobListenerSupport {

    private SchedulerService schedulerService;
    private String name;

    public RetryFailedJobListener(SchedulerService schedulerService, String name) {
        this.schedulerService = schedulerService;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        var map = context.getMergedJobDataMap();
        if(jobException == null || !map.containsKey(JobUtil.Retry.MAX_RETRIES)) {
            // the job succeeded or the job doesn't support retries
            return;
        }

        var retries = map.getInt(JobUtil.Retry.RETRIES);
        var maxRetries = map.getInt(JobUtil.Retry.MAX_RETRIES);
        if(retries >= maxRetries) {
            // the job has exceeded the maximum amount of retries
            return;
        }

        var jobDetail = context.getJobDetail();
        try {
            schedulerService.retry(jobDetail, retries + 1, maxRetries);
        } catch (SchedulerException e) {
            var jobKey = jobDetail.getKey();
            getLog().info("Failed to retry " + jobKey.getGroup() + "." + jobKey.getName(), e);
        }
    }
}
