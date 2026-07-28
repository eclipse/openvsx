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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;

import static org.eclipse.openvsx.cache.CacheService.CACHE_SITEMAP;

@Component
public class PublishExtensionVersionService {

    private static final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionService.class);

    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final StorageUtilService storageUtil;

    public PublishExtensionVersionService(
            RepositoryService repositories,
            EntityManager entityManager,
            StorageUtilService storageUtil
    ) {
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.storageUtil = storageUtil;
    }

    @Transactional
    public void deleteFileResources(ExtensionVersion extVersion) {
        repositories.deleteFiles(extVersion);
    }

    @Retryable
    public void storeResource(TempFile tempFile) {
        storageUtil.uploadFile(tempFile);
    }

    @Transactional
    public void mirrorResource(TempFile tempFile) {
        mirrorResource(tempFile.getResource());
    }

    @Transactional
    public void mirrorResource(FileResource resource) {
        resource.setStorageType(storageUtil.getActiveStorageType());
        entityManager.persist(resource);
    }

    @Transactional
    public void persistResource(FileResource resource) {
        entityManager.persist(resource);
    }

    @Transactional
    public void markExtensionAsPotentiallyMalicious(ExtensionVersion extVersion) {
        extVersion = entityManager.merge(extVersion);
        extVersion.setPotentiallyMalicious(true);
    }

    @Transactional
    @CacheEvict(value = CACHE_SITEMAP, allEntries = true)
    public void activateExtension(ExtensionVersion extVersion, ExtensionService extensions) {
        // Reload the current row before mutating: the passed-in entity may carry a stale snapshot (e.g. it
        // was fetched before a concurrent soft-delete committed), so we must not trust its flags. This
        // matters when a version is soft-deleted while an asynchronous scan for it is still in flight: the
        // scan completing (or being allowed by an admin) must not resurrect the removed version, whose files
        // have already been stripped from storage.
        var current = entityManager.find(ExtensionVersion.class, extVersion.getId());
        if (current == null) {
            // The row was purged (hard-deleted) in the meantime; nothing to activate.
            logger.warn("Refusing to activate missing extension version: {}", NamingUtil.toLogFormat(extVersion));
            return;
        }

        // A soft-deleted version is a permanent tombstone and must never be reactivated.
        if (current.isRemoved()) {
            logger.warn("Refusing to activate removed extension version: {}", NamingUtil.toLogFormat(current));
            return;
        }

        current.setActive(true);
        extensions.updateExtension(current.getExtension());
    }
}
