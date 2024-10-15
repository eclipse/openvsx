/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.MigrationItem;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MigrationService {

    protected final Logger logger = LoggerFactory.getLogger(MigrationService.class);

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final StorageUtilService storageUtil;
    private final JobRequestScheduler scheduler;

    public MigrationService(
            EntityManager entityManager,
            RepositoryService repositories,
            StorageUtilService storageUtil,
            JobRequestScheduler scheduler
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.storageUtil = storageUtil;
        this.scheduler = scheduler;
    }

    @Transactional
    public void enqueueMigration(String jobName, Class<? extends JobRequestHandler<MigrationJobRequest>> handler, MigrationItem item) {
        item = entityManager.merge(item);
        var jobIdText = jobName + "::itemId=" + item.getId();
        var jobId = UUID.nameUUIDFromBytes(jobIdText.getBytes(StandardCharsets.UTF_8));
        scheduler.enqueue(jobId, new MigrationJobRequest<>(handler, item.getEntityId()));
        item.setMigrationScheduled(true);
    }

    public ExtensionVersion getExtension(long entityId) {
        return entityManager.find(ExtensionVersion.class, entityId);
    }

    public FileResource getResource(MigrationJobRequest jobRequest) {
        return entityManager.find(FileResource.class, jobRequest.getEntityId());
    }

    @Transactional
    public void updateResource(FileResource resource) {
        entityManager.merge(resource);
    }

    @Retryable
    public TempFile getExtensionFile(FileResource resource) throws IOException {
        return storageUtil.downloadFile(resource);
    }

    @Retryable
    public void uploadFileResource(TempFile tempFile) {
        storageUtil.uploadFile(tempFile);
    }

    @Retryable
    public void removeFile(FileResource resource) {
        storageUtil.removeFile(resource);
    }

    @Transactional
    public void persistFileResource(FileResource resource) {
        entityManager.persist(resource);
    }

    @Transactional
    public void deleteFileResource(FileResource resource) {
        resource = entityManager.merge(resource);
        entityManager.remove(resource);
    }

    public FileResource getFileResource(ExtensionVersion extVersion, String type) {
        return repositories.findFileByType(extVersion, type);
    }

    public FileResource getDownload(ExtensionVersion extVersion) {
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        if(download == null) {
            logger.warn("Could not find download for: {}", NamingUtil.toLogFormat(extVersion));
        }

        return download;
    }
}
