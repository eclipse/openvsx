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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.quartz.TriggerBuilder.newTrigger;

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

        var schedulerName = getSchedulerName(context);
        if(schedulerName == null) {
            return;
        }

        var jobKey = context.getJobDetail().getKey();
        getLog().debug("CHAIN | jobKey: {}", jobKey);
        var nextJob = repository.findNext(schedulerName, name, jobKey);
        if(nextJob == null) {
            return;
        }

        getLog().info("Job '" + context.getJobDetail().getKey() + "' will now chain to Job '" + nextJob + "'");
        try {
            var instant = LocalDateTime.now()
                    .plus(500, ChronoUnit.MILLIS) // add slight delay so that current job closes db transactions
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
            var trigger = newTrigger()
                    .forJob(nextJob)
                    .startAt(Date.from(instant))
                    .build();
            context.getScheduler().scheduleJob(trigger);
            repository.complete(schedulerName, name, jobKey, nextJob);
        } catch(SchedulerException e) {
            getLog().error("Error encountered during chaining to Job '" + nextJob + "'", e);
        }
    }

    public void addJobChainLink(String schedulerName, JobKey firstJob, JobKey secondJob) {
        getLog().debug("CHAIN | {} {} {} {}", schedulerName, name, firstJob, secondJob);
        if(repository.exists(schedulerName, name, firstJob, secondJob)) {
            getLog().info("Chain already exists {} {} {} {}, skipping", schedulerName, name, firstJob, secondJob);
        } else {
            repository.insert(schedulerName, name, firstJob, secondJob);
        }
    }

    private String getSchedulerName(JobExecutionContext context) {
        try {
            return context.getScheduler().getSchedulerName();
        } catch (SchedulerException e) {
            getLog().error("Error encountered getting scheduler name", e);
            return null;
        }
    }
}
