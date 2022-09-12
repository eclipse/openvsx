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
import org.eclipse.openvsx.util.TargetPlatform;
import org.jooq.DSLContext;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
public class SchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    Scheduler scheduler;

    @Autowired
    AutowiringSpringBeanJobFactory jobFactory;

    @Autowired
    DSLContext dsl;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorModeEnabled;

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.setJobFactory(jobFactory);
        scheduler.getListenerManager().addJobListener(new RetryFailedJobListener(this, "RetryFailedJobs"));
        if(mirrorModeEnabled) {
            var repository = new JobChainingRepository(dsl);
            scheduler.getListenerManager().addJobListener(new JobChainingJobListener("MirrorJobChain", repository), GroupMatcher.groupEquals("Mirror"));
        }
    }

    public void tryChainMirrorJobs(JobKey firstJob, JobKey secondJob) throws SchedulerException {
        if (firstJob == null) {
            var trigger = newTrigger()
                    .withIdentity(new TriggerKey(secondJob.getName() + "Trigger", secondJob.getGroup()))
                    .startAt(Date.from(LocalDateTime.now().plusSeconds(5L).atZone(ZoneId.systemDefault()).toInstant()))
                    .forJob(secondJob)
                    .build();

            scheduler.scheduleJob(trigger);
        } else {
            var jobChain = (JobChainingJobListener) scheduler.getListenerManager().getJobListener("MirrorJobChain");
            jobChain.addJobChainLink(scheduler.getSchedulerName(), firstJob, secondJob);
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
        var triggerId = jobId + "Trigger";

        var triggerKey = new TriggerKey(triggerId, JobUtil.Groups.MIRROR);
        var trigger = scheduler.getTrigger(triggerKey);

        var cronTrigger = newTrigger()
                .withIdentity(triggerKey)
                .startNow()
                .withSchedule(cronSchedule(schedule))
                .build();

        if(trigger != null) {
            scheduler.rescheduleJob(triggerKey, cronTrigger);
            LOGGER.info("++ Rescheduled {} [{}]", jobId, schedule);
        } else {
            var job = newJob(jobClass)
                    .withIdentity(jobId, JobUtil.Groups.MIRROR)
                    .withDescription("Mirror Metadata")
                    .storeDurably()
                    .build();

            scheduler.scheduleJob(job, cronTrigger);
        }
    }

    public void mirrorExtension(String namespace, String extension, String lastModified, int delay) throws SchedulerException {
        var jobId = "MirrorExtension::" + namespace + "." + extension + "::" + lastModified;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        if(scheduler.getJobDetail(jobKey) != null) {
            LOGGER.info("{} already present, skipping", jobKey);
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

        var trigger = newTrigger()
                .withIdentity(jobId + "Trigger", jobKey.getGroup())
                .startAt(Date.from(LocalDateTime.now().plusSeconds(delay * 5L).atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(job, trigger);
        LOGGER.info("++ Added {}", jobId);
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

        var trigger = newTrigger()
                .withIdentity(jobId + "Trigger", jobKey.getGroup())
                .startAt(Date.from(LocalDateTime.now().plusSeconds(5L).atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(job, trigger);
        LOGGER.info("++ Scheduled DeleteExtension::{}.{}", namespace, extension);
    }

    public JobKey generatePublishExtensionVersionJobKey(String namespace, String extension, String version) {
        // TODO add targetPlatform to jobId when mirror supports target platform
        var group = mirrorModeEnabled ? JobUtil.Groups.MIRROR : JobUtil.Groups.PUBLISH;
        var jobId = "PublishExtensionVersion::" + namespace + "." + extension + "-" + version;
        return new JobKey(jobId, group);
    }

    public void publishExtensionVersion(String namespace, String extension, String targetPlatform, String version) throws SchedulerException {
        var jobKey = generatePublishExtensionVersionJobKey(namespace, extension, version);
        var job = setRetryData(newJob(PublishExtensionVersionJob.class), 3)
                .withIdentity(jobKey)
                .withDescription("Publish Extension Version")
                .usingJobData("version", version)
                .usingJobData("targetPlatform", targetPlatform)
                .usingJobData("extension", extension)
                .usingJobData("namespace", namespace)
                .storeDurably()
                .build();

        var trigger = newTrigger()
                .withIdentity(new TriggerKey(jobKey.getName() + "Trigger", jobKey.getGroup()))
                .startAt(Date.from(LocalDateTime.now().plusSeconds(5L).atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public JobKey mirrorExtensionVersion(ExtensionJson json) throws SchedulerException {
        // TODO add targetPlatform to jobId when mirror supports target platform
        var jobId = "MirrorExtensionVersion::" + json.namespace + "." + json.name + "-" + json.version;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
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
                .usingJobData("extension", json.name)
                .usingJobData("version", json.version)
                .usingJobData("targetPlatform", TargetPlatform.NAME_UNIVERSAL) // TODO change when mirror supports target platform
                .usingJobData("timestamp", json.timestamp)
                .storeDurably()
                .build();

        scheduler.addJob(job, false);
        return jobKey;
    }

    public JobKey mirrorActivateExtensionJobKey(String namespace, String extension, String lastModified) {
        var jobId = "MirrorActivateExtension::" + namespace + "." + extension + "::" + lastModified;
        return new JobKey(jobId, JobUtil.Groups.MIRROR);
    }

    public JobKey mirrorActivateExtension(String namespace, String extension, String lastModified) throws SchedulerException {
        var jobKey = mirrorActivateExtensionJobKey(namespace, extension, lastModified);
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

    public void mirrorExtensionMetadata(String namespace, String extension, String timestamp, int delay) throws SchedulerException {
        var jobKey = mirrorExtensionMetadata(namespace, extension, timestamp);
        var trigger = newTrigger()
                .withIdentity(new TriggerKey(jobKey.getName() + "Trigger", jobKey.getGroup()))
                .startAt(Date.from(LocalDateTime.now().plusSeconds(delay * 5L).atZone(ZoneId.systemDefault()).toInstant()))
                .forJob(jobKey)
                .build();

        scheduler.scheduleJob(trigger);
    }

    public JobKey mirrorNamespaceVerified(String namespace, String lastModified) throws SchedulerException {
        var jobId = "MirrorNamespaceVerified::" + namespace + "::" + lastModified;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = setRetryData(newJob(MirrorNamespaceVerifiedJob.class), 10)
                .withIdentity(jobKey)
                .withDescription("Mirror Namespace Verified")
                .usingJobData("namespace", namespace)
                .storeDurably()
                .build();

        scheduler.addJob(job, false);
        return jobKey;
    }

    public void extractResourcesMigration(long id, int delay) throws SchedulerException {
        var jobId = "ExtractResourcesMigration::itemId=" + id;
        var jobKey = new JobKey(jobId, JobUtil.Groups.MIRROR);
        var job = setRetryData(newJob(ExtractResourcesJob.class), 3)
                .withIdentity(jobKey)
                .withDescription("Extract resources from published extension version")
                .usingJobData("itemId", id)
                .storeDurably()
                .build();

        var trigger = newTrigger()
                .withIdentity(new TriggerKey(jobKey.getName() + "Trigger", jobKey.getGroup()))
                .startAt(Date.from(LocalDateTime.now().plusSeconds(delay * 5L).atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public void retry(JobDetail job, int retries, int maxRetries) throws SchedulerException {
        job = setRetryData(job.getJobBuilder(), retries, maxRetries).build();
        var trigger = newTrigger()
                .withIdentity(job.getKey().getName() + "RetryTrigger" + retries, job.getKey().getGroup())
                .startAt(Date.from(LocalDateTime.now().plusSeconds(retries * retries * 5L).atZone(ZoneId.systemDefault()).toInstant()))
                .forJob(job)
                .build();

        scheduler.addJob(job, true);
        scheduler.scheduleJob(trigger);
        LOGGER.info("++ Scheduled {}.{} for retry", job.getKey().getGroup(), job.getKey().getName());
    }

    private JobBuilder setRetryData(JobBuilder jobBuilder, int maxRetries) {
        return setRetryData(jobBuilder, 0, maxRetries);
    }

    private JobBuilder setRetryData(JobBuilder jobBuilder, int retries, int maxRetries) {
        return jobBuilder
                .usingJobData(JobUtil.Retry.RETRIES, retries)
                .usingJobData(JobUtil.Retry.MAX_RETRIES, maxRetries);
    }
}
