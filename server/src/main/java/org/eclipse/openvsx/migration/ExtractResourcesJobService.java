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
import org.eclipse.openvsx.entities.ExtractResourcesMigrationItem;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;

@Component
public class ExtractResourcesJobService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

    public ExtensionVersion getExtension(long itemId) {
        var item = entityManager.find(ExtractResourcesMigrationItem.class, itemId);
        return item.getExtension();
    }

    @Transactional
    public void deleteResources(ExtensionVersion extVersion) {
        repositories.deleteFileResources(extVersion, "resource");
    }

    @Transactional
    public void deleteWebResources(ExtensionVersion extVersion) {
        repositories.deleteFileResources(extVersion, "web-resource");
    }

    @Transactional
    public Map.Entry<FileResource, byte[]> getDownload(ExtensionVersion extVersion) {
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        var content = download.getStorageType().equals(FileResource.STORAGE_DB) ? download.getContent() : null;
        return new AbstractMap.SimpleEntry<>(download, content);
    }

    @Transactional
    public void persistResource(FileResource resource) {
        entityManager.persist(resource);
    }

    @Retryable
    public Path getExtensionFile(Map.Entry<FileResource, byte[]> entry) {
        Path extensionFile;
        try {
            extensionFile = Files.createTempFile("extension_", ".vsix");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create extension file", e);
        }

        var content = entry.getValue();
        if(content == null) {
            var download = entry.getKey();
            var storage = getStorage(download);
            var uri = storage.getLocation(download);
            restTemplate.execute(uri, HttpMethod.GET, null, response -> {
                try(var out = Files.newOutputStream(extensionFile)) {
                    response.getBody().transferTo(out);
                }

                return extensionFile;
            });
        } else {
            try {
                Files.write(extensionFile, content);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to extension file", e);
            }
        }

        return extensionFile;
    }

    @Retryable
    public void uploadResource(FileResource resource) {
        if(resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(resource);
        storage.uploadFile(resource);
        resource.setContent(null);
    }

    private IStorageService getStorage(FileResource resource) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(resource.getStorageType());
    }
}
