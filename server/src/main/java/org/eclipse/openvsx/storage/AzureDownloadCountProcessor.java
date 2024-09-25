/********************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import com.google.common.collect.Lists;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.AzureDownloadCountProcessedItem;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.STORAGE_AZURE;

@Component
public class AzureDownloadCountProcessor {

    protected final Logger logger = LoggerFactory.getLogger(AzureDownloadCountProcessor.class);

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final CacheService cache;
    private final SearchUtilService search;
    private final ObservationRegistry observations;

    public AzureDownloadCountProcessor(
            EntityManager entityManager,
            RepositoryService repositories,
            CacheService cache,
            SearchUtilService search,
            ObservationRegistry observations
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.cache = cache;
        this.search = search;
        this.observations = observations;
    }

    @Transactional
    public void persistProcessedItem(String name, LocalDateTime processedOn, int executionTime, boolean success) {
        Observation.createNotStarted("AzureDownloadCountProcessor#persistProcessedItem", observations).observe(() -> {
            var processedItem = new AzureDownloadCountProcessedItem();
            processedItem.setName(name);
            processedItem.setProcessedOn(processedOn);
            processedItem.setExecutionTime(executionTime);
            processedItem.setSuccess(success);
            entityManager.persist(processedItem);
        });
    }

    public Map<Long, Integer> processDownloadCounts(Map<String, Integer> files) {
        return Observation.createNotStarted("AzureDownloadCountProcessor#processDownloadCounts", observations).observe(() -> {
            return repositories.findDownloadsByStorageTypeAndName(STORAGE_AZURE, files.keySet()).stream()
                    .map(fileResource -> new AbstractMap.SimpleEntry<>(fileResource, files.get(fileResource.getName().toUpperCase())))
                    .collect(Collectors.groupingBy(
                            e -> e.getKey().getExtension().getExtension().getId(),
                            Collectors.summingInt(Map.Entry::getValue)
                    ));
        });
    }

    @Transactional
    public List<Extension> increaseDownloadCounts(Map<Long, Integer> extensionDownloads) {
        return Observation.createNotStarted("AzureDownloadCountProcessor#increaseDownloadCounts", observations).observe(() -> {
            var extensions = repositories.findExtensions(extensionDownloads.keySet()).toList();
            extensions.forEach(extension -> {
                var downloads = extensionDownloads.get(extension.getId());
                extension.setDownloadCount(extension.getDownloadCount() + downloads);
            });

            return extensions;
        });
    }

    @Transactional //needs transaction for lazy-loading versions
    public void evictCaches(List<Extension> extensions) {
        Observation.createNotStarted("AzureDownloadCountProcessor#evictCaches", observations).observe(() -> {
            extensions.forEach(extension -> {
                extension = entityManager.merge(extension);
                cache.evictExtensionJsons(extension);
                cache.evictLatestExtensionVersion(extension);
            });
        });
    }

    public void updateSearchEntries(List<Extension> extensions) {
        Observation.createNotStarted("AzureDownloadCountProcessor#updateSearchEntries", observations).observe(() -> {
            logger.info(">> updateSearchEntries");
            var activeExtensions = extensions.stream()
                    .filter(Extension::isActive)
                    .collect(Collectors.toList());

            logger.info("total active extensions: {}", activeExtensions.size());
            var parts = Lists.partition(activeExtensions, 100);
            logger.info("partitions: {} | partition size: 100", parts.size());

            parts.forEach(search::updateSearchEntriesAsync);
            logger.info("<< updateSearchEntries");
        });
    }

    public List<String> processedItems(List<String> blobNames) {
        return Observation.createNotStarted("AzureDownloadCountProcessor#processedItems", observations).observe(() -> {
            return repositories.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(blobNames);
        });
    }
}
