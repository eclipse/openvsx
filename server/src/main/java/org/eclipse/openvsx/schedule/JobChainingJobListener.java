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
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;

public class JobChainingJobListener extends JobListenerSupport {

    private String name;
    private JobChainingRepository repository;

    public JobChainingJobListener(String name, JobChainingRepository repository) {
        this.name = name;
        this.repository = repository;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if(jobException != null) {
            // the job failed
            return;
        }

        var jobKey = context.getJobDetail().getKey();
        String schedulerName = null;
        try {
            schedulerName = context.getScheduler().getSchedulerName();
        } catch (SchedulerException e) {
            getLog().error("Error encountered getting scheduler name", e);
        }

        var nextJob = repository.findNext(schedulerName, name, jobKey);
        if(nextJob == null) {
            return;
        }

        getLog().info("Job '" + context.getJobDetail().getKey() + "' will now chain to Job '" + nextJob + "'");
        try {
            context.getScheduler().triggerJob(nextJob);
            repository.complete(schedulerName, name, jobKey, nextJob);
        } catch(SchedulerException e) {
            getLog().error("Error encountered during chaining to Job '" + nextJob + "'", e);
        }
    }

    public void addJobChainLink(String schedulerName, JobKey firstJob, JobKey secondJob) {
        repository.insert(schedulerName, name, firstJob, secondJob);
    }
}
