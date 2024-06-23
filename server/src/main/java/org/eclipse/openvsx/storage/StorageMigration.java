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

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

import static org.eclipse.openvsx.entities.FileResource.*;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class StorageMigration {

    protected final Logger logger = LoggerFactory.getLogger(StorageMigration.class);

    private final TaskScheduler taskScheduler;
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final StorageUtilService storageUtil;
    private final RestTemplate backgroundRestTemplate;
    private final ConcurrentLinkedQueue<Long> resourceQueue;
    private ScheduledFuture<?> scheduledFuture;

    @Value("${ovsx.storage.migration-delay:500}")
    long migrationDelay;

    public StorageMigration(
            TaskScheduler taskScheduler,
            TransactionTemplate transactions,
            EntityManager entityManager,
            RepositoryService repositories,
            StorageUtilService storageUtil,
            RestTemplate backgroundRestTemplate
    ) {
        this.taskScheduler = taskScheduler;
        this.transactions = transactions;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.storageUtil = storageUtil;
        this.backgroundRestTemplate = backgroundRestTemplate;
        this.resourceQueue = new ConcurrentLinkedQueue<>();
    }

    @EventListener
    public void findResources(ApplicationStartedEvent event) {
        var storageType = storageUtil.getActiveStorageType();
        if (storageType.equals(STORAGE_DB)) {
            // No migration is performed if we store resources in the database
            return;
        }

        var migrations = new ArrayList<>(List.of(STORAGE_DB, STORAGE_GOOGLE, STORAGE_AZURE));
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
            var duration = Duration.of(migrationDelay, ChronoUnit.MILLIS);
            scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::migrateResources, duration);
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
        return backgroundRestTemplate.getForObject("{extensionLocation}", byte[].class, Map.of("extensionLocation", location));
    }

}