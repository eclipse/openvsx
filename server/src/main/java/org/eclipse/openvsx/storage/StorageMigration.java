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

import static org.eclipse.openvsx.entities.FileResource.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

import javax.persistence.EntityManager;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

@Component
public class StorageMigration {

    protected final Logger logger = LoggerFactory.getLogger(StorageMigration.class);

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.storage.migration-delay:500}")
    long migrationDelay;

    private final ConcurrentLinkedQueue<Long> resourceQueue = new ConcurrentLinkedQueue<>();
    private ScheduledFuture<?> scheduledFuture;

    @EventListener
    public void findResources(ApplicationStartedEvent event) {
        var storageType = storageUtil.getActiveStorageType();
        if (storageType.equals(STORAGE_DB)) {
            // No migration is performed if we store resources in the database
            return;
        }

        var migrations = Lists.newArrayList(STORAGE_DB, STORAGE_GOOGLE, STORAGE_AZURE);
        migrations.remove(storageType);
        var migrationCount = new int[migrations.size()];
        for (var i = 0; i < migrations.size(); i++) {
            final int index = i;
            repositories.findFilesByStorageType(migrations.get(index))
                .filter(resource -> !resource.getStorageType().equals(STORAGE_DB)
                                    || storageUtil.shouldStoreExternally(resource))
                .forEach(resource -> {
                    resourceQueue.add(resource.getId());
                    migrationCount[index]++;
                });
        }
        
        if (!resourceQueue.isEmpty()) {
            for (var i = 0; i < migrations.size(); i++) {
                if (migrationCount[i] > 0) {
                    logger.info("Migrating " + migrationCount[i] + " resources from "
                            + migrations.get(i) + " to " + storageType + ".");
                }
            }
            scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::migrateResources, migrationDelay);
        }
    }

    public void migrateResources() {
        var resourceId = resourceQueue.poll();
        if (resourceId == null) {
            logger.info("Completed migration of resources.");
            scheduledFuture.cancel(false);
            return;
        }

        transactions.<Void>execute(status -> {
            var resource = entityManager.find(FileResource.class, resourceId);
            if (resource == null)
                return null;

            if (!resource.getStorageType().equals(STORAGE_DB)) {
                resource.setContent(downloadFile(resource));
            }
            storageUtil.uploadFile(resource);
            resource.setContent(null);
            return null;
        });

        var remainingCount = resourceQueue.size();
        if (remainingCount > 0 && remainingCount % 1000 == 0) {
            logger.info("Remaining resources to migrate: " + remainingCount);
        }
    }

    private byte[] downloadFile(FileResource resource) {
        var location = storageUtil.getLocation(resource);
        return restTemplate.getForObject(location, byte[].class);
    }

}