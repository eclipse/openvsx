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

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.migration.ExtractResourcesJob;
import org.eclipse.openvsx.mirror.*;
import org.eclipse.openvsx.publish.PublishExtensionVersionJob;
import org.jooq.DSLContext;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.BroadcastJobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Map;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
public class SchedulerService {

    protected final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    Scheduler scheduler;

    @Autowired
    AutowiringSpringBeanJobFactory jobFactory;

    @Autowired
    DSLContext dsl;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorModeEnabled;

    private JobQueueJobListener jobQueueJobListener;
    private JobChainingJobListener jobChainingJobListener;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.setJobFactory(jobFactory);
        var listeners = new ArrayList<JobListener>();
        listeners.add(new RetryFailedJobListener(this, "RetryFailedJobs"));
        if(mirrorModeEnabled) {
            jobChainingJobListener = new JobChainingJobListener("MirrorJobChain", new JobChainingRepository(dsl));
            listeners.add(jobChainingJobListener);
        }

        jobQueueJobListener = new JobQueueJobListener("JobQueue", new JobQueueRepository(dsl));
        listeners.add(jobQueueJobListener);
        scheduler.getListenerManager().addJobListener(new BroadcastJobListener("Broadcaster", listeners));
    }

    public void tryChainMirrorJobs(JobKey firstJob, JobKey secondJob) throws SchedulerException {
        if (firstJob == null) {
            enqueueJob(secondJob);
        } else {
            jobChainingJobListener.addJobChainLink(scheduler.getSchedulerName(), firstJob, secondJob);
        }
    }

    public void unscheduleMirrorJobs() throws SchedulerException {
        var triggerKeys = scheduler.getTriggerKeys(GroupMatcher.groupEquals(JobUtil.Groups.MIRROR));
        scheduler.unscheduleJobs(new ArrayList<>(triggerKeys));
    }

    public void mirrorSitemap(String schedule) throws SchedulerException {
        scheduleCronJob("MirrorSitemap", schedule, MirrorSitemapJob.class);
    }

    public void mirrorMetadata(String schedule) throws SchedulerException {
        scheduleCronJob("MirrorMetadata", schedule, MirrorMetadataJob.class);
    }

    private void scheduleCronJob(String jobName, String schedule, Class<? extends Job> jobClass) throws SchedulerException {
        var jobId = jobName + "Job";
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = scheduler.getJobDetail(jobKey);
        if(job == null) {
            job = newJob(jobClass)
                    .withIdentity(jobKey)
                    .withDescription("Mirror Metadata")
                    .storeDurably()
                    .build();

            scheduler.addJob(job, false);
        }

        var triggerId = jobId + "Trigger";
        var triggerKey = new TriggerKey(triggerId, JobUtil.Groups.MIRROR);
        var trigger = scheduler.getTrigger(triggerKey);
        var cronTrigger = newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startNow()
                .withSchedule(cronSchedule(schedule))
                .build();

        if(trigger != null) {
            scheduler.rescheduleJob(triggerKey, cronTrigger);
            logger.info("++ Rescheduled {} [{}]", jobId, schedule);
        } else {
            scheduler.scheduleJob(cronTrigger);
            logger.info("++ Scheduled {} [{}]", jobId, schedule);
        }
    }

    public void mirrorExtension(String namespace, String extension, String lastModified) throws SchedulerException {
        var jobId = "MirrorExtension::" + namespace + "." + extension + "::" + lastModified;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        if(scheduler.getJobDetail(jobKey) != null) {
            logger.warn("{} already present, skipping", jobKey);
            return;
        }

        var job = setRetryData(newJob(MirrorExtensionJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Mirror Extension")
                .usingJobData("namespace", namespace)
                .usingJobData("extension", extension)
                .usingJobData("lastModified", lastModified)
                .storeDurably()
                .build();

        enqueueJob(job, false);
    }

    public void mirrorDeleteExtension(String namespace, String extension, long timestamp) throws SchedulerException {
        var jobId = "DeleteExtension::" + namespace + "." + extension + "::" + timestamp;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = setRetryData(newJob(DeleteExtensionJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Delete Extension")
                .usingJobData("namespace", namespace)
                .usingJobData("extension", extension)
                .storeDurably()
                .build();

        enqueueJob(job, false);
    }

    public JobKey generatePublishExtensionVersionJobKey(String namespace, String extension, String targetPlatform, String version) {
        var group = mirrorModeEnabled ? JobUtil.Groups.MIRROR : JobUtil.Groups.PUBLISH;
        var jobId = "PublishExtensionVersion::" + namespace + "." + extension + "-" + version + "@" + targetPlatform;
        return new JobKey(jobId, group);
    }

    public void publishExtensionVersion(String namespace, String extension, String targetPlatform, String version) throws SchedulerException {
        var jobKey = generatePublishExtensionVersionJobKey(namespace, extension, targetPlatform, version);
        var job = setRetryData(newJob(PublishExtensionVersionJob.class), 3)
                .withIdentity(jobKey)
                .withDescription("Publish Extension Version")
                .usingJobData("version", version)
                .usingJobData("targetPlatform", targetPlatform)
                .usingJobData("extension", extension)
                .usingJobData("namespace", namespace)
                .storeDurably()
                .build();

        enqueueJob(job);
    }

    public JobKey mirrorExtensionVersion(ExtensionJson json) throws SchedulerException {
        var jobId = "MirrorExtensionVersion::" + json.namespace + "." + json.name + "-" + json.version + "@" + json.targetPlatform;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        if (scheduler.getJobDetail(jobKey) == null) {
            var job = setRetryData(newJob(MirrorExtensionVersionJob.class), 10)
                    .withIdentity(jobKey)
                    .withDescription("Mirror Extension Version")
                    .usingJobData("download", json.files.get("download"))
                    .usingJobData("userProvider", json.publishedBy.provider)
                    .usingJobData("userLoginName", json.publishedBy.loginName)
                    .usingJobData("userFullName", json.publishedBy.fullName)
                    .usingJobData("userAvatarUrl", json.publishedBy.avatarUrl)
                    .usingJobData("userHomepage", json.publishedBy.homepage)
                    .usingJobData("namespace", json.namespace)
                    .storeDurably()
                    .build();

            scheduler.addJob(job, false);
        }

        return jobKey;
    }

    public JobKey mirrorActivateExtensionJobKey(String namespace, String extension, String lastModified) {
        var jobId = "MirrorActivateExtension::" + namespace + "." + extension + "::" + lastModified;
        return new JobKey(jobId, JobUtil.Groups.MIRROR);
    }

    public JobKey mirrorActivateExtension(String namespace, String extension, String lastModified) throws SchedulerException {
        var jobKey = mirrorActivateExtensionJobKey(namespace, extension, lastModified);
        if(scheduler.getJobDetail(jobKey) != null) {
            logger.warn("{} already present, skipping", jobKey);
            return jobKey;
        }

        var job = setRetryData(newJob(MirrorActivateExtensionJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Activate Extension")
                .usingJobData("namespace", namespace)
                .usingJobData("extension", extension)
                .storeDurably()
                .build();

        scheduler.addJob(job, false);
        return jobKey;
    }

    public JobKey mirrorExtensionMetadata(String namespace, String extension, String lastModified) throws SchedulerException {
        var jobId = "MirrorExtensionMetadata::" + namespace + "." + extension + "::" + lastModified;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        if(scheduler.getJobDetail(jobKey) != null) {
            logger.warn("{} already present, skipping", jobKey);
            return jobKey;
        }

        var job = setRetryData(newJob(MirrorExtensionMetadataJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Mirror Extension Metadata")
                .usingJobData("namespace", namespace)
                .usingJobData("extension", extension)
                .storeDurably()
                .build();

        scheduler.addJob(job, false);
        return jobKey;
    }

    public JobKey mirrorNamespaceVerified(String namespace, String lastModified) throws SchedulerException {
        var jobId = "MirrorNamespaceVerified::" + namespace + "::" + lastModified;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        if(scheduler.getJobDetail(jobKey) != null) {
            logger.warn("{} already present, skipping", jobKey);
            return jobKey;
        }

        var job = setRetryData(newJob(MirrorNamespaceVerifiedJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Mirror Namespace Verified")
                .usingJobData("namespace", namespace)
                .storeDurably()
                .build();

        scheduler.addJob(job, false);
        return jobKey;
    }

    public void extractResourcesMigration(long id) throws SchedulerException {
        var jobId = "ExtractResourcesMigration::itemId=" + id;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = setRetryData(newJob(ExtractResourcesJob.class), 3)
                .withIdentity(jobKey)
                .withDescription("Extract resources from published extension version")
                .usingJobData("itemId", id)
                .storeDurably()
                .build();

        enqueueJob(job);
    }

    public void retry(JobDetail job, int retries, int maxRetries) throws SchedulerException {
        job = setRetryData(job.getJobBuilder(), retries, maxRetries).build();
        requeueJob(job);
    }

    private JobBuilder setRetryData(JobBuilder jobBuilder, int maxRetries) {
        return setRetryData(jobBuilder, 0, maxRetries);
    }

    private JobBuilder setRetryData(JobBuilder jobBuilder, int retries, int maxRetries) {
        return jobBuilder
                .usingJobData(JobUtil.Retry.RETRIES, retries)
                .usingJobData(JobUtil.Retry.MAX_RETRIES, maxRetries);
    }

    private void requeueJob(JobDetail job) throws SchedulerException {
        enqueueJob(job, true);
    }

    private void enqueueJob(JobDetail job) throws SchedulerException {
        enqueueJob(job, false);
    }

    private void enqueueJob(JobDetail job, boolean replace) throws SchedulerException {
        scheduler.addJob(job, replace);
        enqueueJob(job.getKey(), getPriority(job), replace);
    }

    private void enqueueJob(JobKey jobKey) throws SchedulerException {
        enqueueJob(jobKey, getPriority(jobKey), false);
    }

    private void enqueueJob(JobKey jobKey, int priority, boolean replace) throws SchedulerException {
        jobQueueJobListener.queueJob(scheduler, jobKey, priority, replace);
        logger.info("Added {}", jobKey);
    }

    private int getPriority(JobKey jobKey) throws SchedulerException {
        return getPriority(scheduler.getJobDetail(jobKey));
    }

    private int getPriority(JobDetail job) {
        var jobPriorities = Map.of(
                MirrorActivateExtensionJob.class, 0,
                MirrorExtensionMetadataJob.class, 1,
                MirrorNamespaceVerifiedJob.class, 2,
                PublishExtensionVersionJob.class, 3,
                MirrorExtensionVersionJob.class, 4,
                ExtractResourcesJob.class, 5,
                DeleteExtensionJob.class, 5,
                MirrorSitemapJob.class, 5,
                MirrorMetadataJob.class, 5,
                MirrorExtensionJob.class, 5
        );
        return jobPriorities.get(job.getJobClass());
    }
}
