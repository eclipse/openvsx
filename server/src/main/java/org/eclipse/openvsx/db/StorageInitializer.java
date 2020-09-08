/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.db;

import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StorageInitializer {

    protected final Logger logger = LoggerFactory.getLogger(StorageInitializer.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @EventListener
    @Transactional
    public void initFileStorage(ApplicationStartedEvent event) {
        var updated = new int[2];
        repositories.findAllFiles().forEach(resource -> {
            if (storageUtil.shouldStoreExternally(resource) && googleStorage.isEnabled()) {
                if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
                    googleStorage.uploadFile(resource);
                    resource.setContent(null);
                    updated[0]++;
                }
            } else if (resource.getUrl() == null) {
                var extVersion = resource.getExtension();
                var extension = extVersion.getExtension();
                var namespace = extension.getNamespace();
                resource.setUrl(UrlUtil.createApiUrl("", "api", namespace.getName(), extension.getName(), extVersion.getVersion(),
                        "file", resource.getName()));
                updated[1]++;
            }
        });

        if (updated[0] > 0)
            logger.info("Uploaded " + updated[0] + " extensions to Google Cloud Storage.");
        if (updated[1] > 0)
            logger.info("Updated " + updated[1] + " resource URLs.");
    }

}