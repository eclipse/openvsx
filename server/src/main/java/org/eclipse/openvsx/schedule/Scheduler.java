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

import org.eclipse.openvsx.migration.ExtractResourcesJobRequest;
import org.eclipse.openvsx.mirror.*;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    JobRequestScheduler scheduler;

    @Autowired
    RecurringJobRepository recurringJobs;

    @Autowired
    EntityManager entityManager;

    @Transactional
    public void scheduleMirrorExtensions(String schedule) {
        scheduleRecurringJob("MirrorExtensions", schedule, new MirrorSitemapJobRequest());
    }

    @Transactional
    public void deleteMirrorExtensions() {
        deleteRecurringJob("MirrorExtensions");
    }

    @Transactional
    public void scheduleMirrorMetadata(String schedule) {
        scheduleRecurringJob("MirrorMetadata", schedule, new MirrorMetadataJobRequest());
    }

    @Transactional
    public void deleteMirrorMetadata() {
        deleteRecurringJob("MirrorMetadata");
    }

    public void enqueueExtractResourcesMigration(long id) {
        scheduler.enqueue(toUUID("ExtractResourcesMigration::itemId=" + id), new ExtractResourcesJobRequest(id));
        LOGGER.info("++ Scheduled ExtractResourcesMigration::itemId={}", id);
    }

    public void enqueueMirrorExtensionMetadata(String namespace, String extension, String timestamp) {
        var jobId = toUUID("MirrorExtensionMetadata::" + namespace + "." + extension + "::" + timestamp);
        scheduler.enqueue(jobId, new MirrorExtensionMetadataJobRequest(namespace, extension));
        LOGGER.info("++ Scheduled MirrorExtensionMetadata::{}.{}::{}", namespace, extension, timestamp);
    }

    public void enqueueMirrorExtension(String namespace, String extension, String lastModified) {
        var jobId = toUUID("MirrorExtension::" + namespace + "." + extension + "::" + lastModified);
        scheduler.enqueue(jobId, new MirrorExtensionJobRequest(namespace, extension));
        LOGGER.info("++ Scheduled MirrorExtension::{}.{}::{}", namespace, extension, lastModified);
    }

    public void enqueueDeleteExtension(String namespace, String extension) {
        var jobId = toUUID("DeleteExtension::" + namespace + "." + extension);
        scheduler.enqueue(jobId, new DeleteExtensionJobRequest(namespace, extension));
        LOGGER.info("++ Scheduled DeleteExtension::{}.{}", namespace, extension);
    }

    private void scheduleRecurringJob(String prefix, String schedule, JobRequest jobRequest) {
        var job = recurringJobs.findByPrefix(prefix);
        if(job != null && job.getSchedule().equals(schedule)) {
            // job with same schedule already exists
            // nothing to do here
            return;
        }

        var jobId = toRecurringJobId(prefix, schedule);
        jobId = scheduler.scheduleRecurrently(jobId, schedule, jobRequest);
        if(job == null) {
            job = new RecurringJob();
            job.setPrefix(prefix);
            job.setJobId(jobId);
            job.setSchedule(schedule);
            entityManager.persist(job);
        } else {
            scheduler.delete(job.getJobId());
            LOGGER.info("-- Deleted {} [{}] ({})", job.getPrefix(), job.getSchedule(), job.getJobId());
            job.setJobId(jobId);
            job.setSchedule(schedule);
        }

        LOGGER.info("++ Scheduled {} [{}] ({})", prefix, schedule, jobId);
    }

    private void deleteRecurringJob(String prefix) {
        var job = recurringJobs.findByPrefix(prefix);
        if(job != null) {
            scheduler.delete(job.getJobId());
            entityManager.remove(job);
            LOGGER.info("-- Deleted {} [{}] ({})", prefix, job.getSchedule(), job.getJobId());
        }
    }

    private String toRecurringJobId(String id, String schedule) {
        return toUUID(id + "::schedule=" + schedule).toString();
    }

    private UUID toUUID(String jobId) {
        return UUID.nameUUIDFromBytes(jobId.getBytes(StandardCharsets.UTF_8));
    }
}
