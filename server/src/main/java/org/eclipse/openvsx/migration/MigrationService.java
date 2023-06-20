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

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.MigrationItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MigrationService {

    @Autowired
    RestTemplate backgroundRestTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Autowired
    JobRequestScheduler scheduler;

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

    @Retryable
    public TempFile getExtensionFile(Map.Entry<FileResource, byte[]> entry) throws IOException {
        var extensionFile = new TempFile("migration-extension_", ".vsix");

        var content = entry.getValue();
        if(content == null) {
            var download = entry.getKey();
            var storage = getStorage(download);
            var uri = storage.getLocation(download);
            backgroundRestTemplate.execute("{extensionLocation}", HttpMethod.GET, null, response -> {
                try(var out = Files.newOutputStream(extensionFile.getPath())) {
                    response.getBody().transferTo(out);
                }

                return extensionFile;
            }, Map.of("extensionLocation", uri.toString()));
        } else {
            Files.write(extensionFile.getPath(), content);
        }

        return extensionFile;
    }

    @Retryable
    public void uploadFileResource(FileResource resource) {
        if(resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(resource);
        storage.uploadFile(resource);
        resource.setContent(null);
    }

    @Retryable
    public void uploadFileResource(FileResource resource, TempFile extensionFile) {
        if(resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(resource);
        storage.uploadFile(resource, extensionFile);
        resource.setContent(null);
    }

    @Retryable
    public void removeFile(FileResource resource) {
        if(!resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var storage = getStorage(resource);
            storage.removeFile(resource);
        }
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

    @Transactional
    public Map.Entry<FileResource, byte[]> getDownload(ExtensionVersion extVersion) {
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        var content = download.getStorageType().equals(FileResource.STORAGE_DB) ? download.getContent() : null;
        return new AbstractMap.SimpleEntry<>(download, content);
    }

    @Transactional
    public byte[] getContent(FileResource download) {
        download = entityManager.merge(download);
        return download.getStorageType().equals(FileResource.STORAGE_DB) ? download.getContent() : null;
    }

    private IStorageService getStorage(FileResource resource) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(resource.getStorageType());
    }
}
