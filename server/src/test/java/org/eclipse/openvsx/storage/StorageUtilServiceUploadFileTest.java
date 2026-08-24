/** ******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.storage;

import java.nio.file.Files;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.metrics.ExtensionDownloadMetrics;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.log.DownloadCountService;
import org.eclipse.openvsx.util.TempFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StorageUtilService#uploadFile(TempFile)}.
 */
@ExtendWith(MockitoExtension.class)
class StorageUtilServiceUploadFileTest {

    @Mock
    RepositoryService repositories;
    @Mock
    GoogleCloudStorageService googleStorage;
    @Mock
    AzureBlobStorageService azureStorage;
    @Mock
    LocalStorageService localStorage;
    @Mock
    AwsStorageService awsStorage;
    @Mock
    DownloadCountService downloadCountService;
    @Mock
    ExtensionDownloadMetrics downloadMetrics;
    @Mock
    SearchUtilService search;
    @Mock
    org.eclipse.openvsx.cache.CacheService cache;
    @Mock
    EntityManager entityManager;
    @Mock
    FileCacheDurationConfig fileCacheDurationConfig;
    @Mock
    CdnServiceConfig cdnServiceConfig;

    @Test
    void uploadFile_recordsTheSizeOfTheUploadedBytes() throws Exception {
        when(localStorage.isEnabled()).thenReturn(true);

        var svc = new StorageUtilService(
                repositories,
                googleStorage,
                azureStorage,
                localStorage,
                awsStorage,
                downloadCountService,
                downloadMetrics,
                search,
                cache,
                entityManager,
                fileCacheDurationConfig,
                cdnServiceConfig);

        var resource = new FileResource();
        try (var tempFile = new TempFile("upload_", ".tmp")) {
            tempFile.setResource(resource);
            Files.writeString(tempFile.getPath(), "extension package bytes");

            svc.uploadFile(tempFile);

            assertThat(resource.getStorageType()).isEqualTo(FileResource.STORAGE_LOCAL);
            assertThat(resource.getSize()).isEqualTo(Files.size(tempFile.getPath()));
        }
    }
}
