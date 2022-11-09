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

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class MigrationService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

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

    @Retryable
    public void uploadResource(FileResource resource, Path extensionFile) {
        if(resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(resource);
        storage.uploadFile(resource, extensionFile);
        resource.setContent(null);
    }

    @Retryable
    public void deleteResource(FileResource resource) {
        if(resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            return;
        }

        var storage = getStorage(resource);
        storage.removeFile(resource);
    }

    private IStorageService getStorage(FileResource resource) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(resource.getStorageType());
    }
}
