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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.SetPreReleaseMigrationItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.slf4j.Logger;
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
import java.util.List;
import java.util.Map;

@Component
public class SetPreReleaseJobService {

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

    @Transactional
    public List<ExtensionVersion> getExtensionVersions(SetPreReleaseJobRequest jobRequest, Logger logger) {
        var item = entityManager.find(SetPreReleaseMigrationItem.class, jobRequest.getItemId());
        var extension = item.getExtension();
        logger.info("Setting pre-release for: {}.{}", extension.getNamespace().getName(), extension.getName());
        return extension.getVersions();
    }

    @Transactional
    public Map.Entry<FileResource, byte[]> getDownload(ExtensionVersion extVersion) {
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        var content = download.getStorageType().equals(FileResource.STORAGE_DB) ? download.getContent() : null;
        return new AbstractMap.SimpleEntry<>(download, content);
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

    @Transactional
    public void updatePreviewAndPreRelease(ExtensionVersion extVersion, Path extensionFile) {
        try(var extProcessor = new ExtensionProcessor(extensionFile)) {
            extVersion.setPreRelease(extProcessor.isPreRelease());
            extVersion.setPreview(extProcessor.isPreview());
        }

        entityManager.merge(extVersion);
    }

    private IStorageService getStorage(FileResource resource) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(resource.getStorageType());
    }
}
