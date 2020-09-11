/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
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
        repositories.findFilesByStorageType(FileResource.STORAGE_DB).forEach(resource -> {
            if (storageUtil.shouldStoreExternally(resource) && googleStorage.isEnabled()) {
                googleStorage.uploadFileNewTx(resource);
                updated[0]++;
            } else if (resource.getUrl() == null) {
                storageUtil.setInternalFileUrl(resource);
                updated[1]++;
            }
        });

        if (updated[0] > 0)
            logger.info("Uploaded " + updated[0] + " resources to Google Cloud Storage.");
        if (updated[1] > 0)
            logger.info("Updated " + updated[1] + " resource URLs.");
    }

}