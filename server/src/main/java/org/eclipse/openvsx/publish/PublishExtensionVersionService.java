/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class PublishExtensionVersionService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    StorageUtilService storageUtil;

    @Transactional
    public void deleteFileResources(ExtensionVersion extVersion) {
        repositories.findFiles(extVersion).forEach(entityManager::remove);
    }

    public void storeDownload(FileResource download, Path extensionFile) {
        if (storageUtil.shouldStoreExternally(download)) {
            storageUtil.uploadFile(download, extensionFile);
        } else {
            try {
                download.setContent(Files.readAllBytes(extensionFile));
            } catch (IOException e) {
                throw new ErrorResultException("Failed to read extension file", e);
            }

            download.setStorageType(FileResource.STORAGE_DB);
        }
    }

    @Retryable
    public void storeResource(FileResource resource) {
        // Store file resource in the DB or external storage
        if (storageUtil.shouldStoreExternally(resource)) {
            storageUtil.uploadFile(resource);
            // Don't store the binary content in the DB - it's now stored externally
            resource.setContent(null);
        } else {
            resource.setStorageType(FileResource.STORAGE_DB);
        }
    }

    @Transactional
    public void mirrorResource(FileResource resource) {
        resource.setStorageType(storageUtil.getActiveStorageType());
        // Don't store the binary content in the DB - it's now stored externally
        resource.setContent(null);
        entityManager.persist(resource);
    }

    @Transactional
    public void persistResource(FileResource resource) {
        resource.setExtension(entityManager.merge(resource.getExtension()));
        entityManager.persist(resource);
    }

    @Transactional
    public void activateExtension(ExtensionVersion extVersion, ExtensionService extensions) {
        extVersion.setActive(true);
        extVersion = entityManager.merge(extVersion);
        extensions.updateExtension(extVersion.getExtension());
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateExtensionPublicId(Extension extension) {
        entityManager.merge(extension);
    }
}
