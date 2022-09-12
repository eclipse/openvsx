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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.ExtractResourcesMigrationItem;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class ExtractResourcesService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Transactional(rollbackFor = Exception.class)
    public void extractResources(long itemId) throws IOException {
        var item = entityManager.find(ExtractResourcesMigrationItem.class, itemId);
        var extVersion = item.getExtension();
        repositories.deleteFileResources(extVersion, "resource");
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        processEachResource(download, (resource) -> {
            uploadResource(resource);
            entityManager.persist(resource);
        });

        repositories.deleteFileResources(extVersion, "web-resource");
    }

    private void processEachResource(FileResource download, Consumer<FileResource> processor) throws IOException {
        byte[] content;
        if(download.getStorageType().equals(FileResource.STORAGE_DB)) {
            content = download.getContent();
        } else {
            var storage = getStorage(download);
            var uri = storage.getLocation(download);
            content = restTemplate.getForObject(uri, byte[].class);
        }

        try(var input = new ByteArrayInputStream(content)) {
            try(var extProcessor = new ExtensionProcessor(input)) {
                extProcessor.processEachResource(download.getExtension(), (resource) -> {
                    resource.setStorageType(download.getStorageType());
                    processor.accept(resource);
                });
            }
        }
    }

    private void uploadResource(FileResource resource) {
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
