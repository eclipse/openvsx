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

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobListenerSupport;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.jdbcjobstore.Constants.STATE_COMPLETE;
import static org.quartz.impl.jdbcjobstore.Constants.STATE_WAITING;

public class JobQueueJobListener extends JobListenerSupport {

    private String name;
    private JobQueueRepository repository;

    public JobQueueJobListener(String name, JobQueueRepository repository) {
        this.name = name;
        this.repository = repository;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            triggerJobs(context.getScheduler());
        } catch(SchedulerException e) {
            getLog().error("Error encountered during queueing of Jobs", e);
        }
    }

    public void queueJob(Scheduler scheduler, JobKey jobKey, int priority, boolean replace) throws SchedulerException {
        var schedulerName = scheduler.getSchedulerName();
        getLog().debug("QUEUE | {} {} {} {}", schedulerName, name, jobKey, priority);
        var exists = repository.exists(schedulerName, name, jobKey);
        getLog().debug("exists: {} | replace: {}", exists, replace);
        if(exists && replace) {
            repository.updateState(schedulerName, name, jobKey, STATE_WAITING);
        } else if(exists) {
            getLog().info("Job already exists in queue {} {} {}, skipping", schedulerName, name, jobKey);
        } else {
            repository.insert(schedulerName, name, jobKey, priority);
        }

        triggerJobs(scheduler);
    }

    private synchronized void triggerJobs(Scheduler scheduler) throws SchedulerException {
        var schedulerName = getSchedulerName(scheduler);
        if(schedulerName == null) {
            return;
        }

        var jobCount = 0;
        for(var triggerKey : scheduler.getTriggerKeys(GroupMatcher.anyGroup())) {
            var state = scheduler.getTriggerState(triggerKey);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            var nextFireTime = trigger != null ? trigger.getFireTimeAfter(Date.from(LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant())) : null;
            getLog().debug("{}\t| {}, {}", triggerKey, state, nextFireTime);
            if(state.equals(Trigger.TriggerState.NORMAL) && nextFireTime == null) {
                jobCount++;
            }
        }

        var maxJobCount = scheduler.getMetaData().getThreadPoolSize();
        var limit = maxJobCount - jobCount;
        getLog().debug("maxJobs: {}, jobs: {}, limit: {}", maxJobCount, jobCount, limit);
        if(limit <= 0) {
            return;
        }

        var nextJobs = repository.findNext(schedulerName, name, limit);
        for(var nextJob : nextJobs) {
            getLog().debug("QUEUE | nextJob: {}", nextJob);
            var instant = LocalDateTime.now()
                    .plus(500, ChronoUnit.MILLIS) // add slight delay so that current job closes db transactions
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
            var trigger = newTrigger()
                    .forJob(nextJob)
                    .startAt(Date.from(instant))
                    .build();
            scheduler.scheduleJob(trigger);
            repository.updateState(schedulerName, name, nextJob, STATE_COMPLETE);
        }
    }

    private String getSchedulerName(Scheduler scheduler) {
        try {
            return scheduler.getSchedulerName();
        } catch (SchedulerException e) {
            getLog().error("Error encountered getting scheduler name", e);
            return null;
        }
    }
}
