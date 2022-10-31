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

import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.STORAGE_AZURE;

@Component
public class AzureDownloadCountProcessor {

    protected final Logger logger = LoggerFactory.getLogger(AzureDownloadCountProcessor.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    DownloadCountService downloadCounts;

    @Transactional
    public void persistProcessedItem(String name, LocalDateTime processedOn, int executionTime, boolean success) {
        var processedItem = new AzureDownloadCountProcessedItem();
        processedItem.setName(name);
        processedItem.setProcessedOn(processedOn);
        processedItem.setExecutionTime(executionTime);
        processedItem.setSuccess(success);
        entityManager.persist(processedItem);
    }

    public Map<Long, List<Download>> processDownloadCounts(Map<String, List<LocalDateTime>> files) {
        return repositories.findDownloadsByStorageTypeAndName(STORAGE_AZURE, files.keySet()).stream()
                .map(fileResource -> new AbstractMap.SimpleEntry<>(fileResource, toDownloads(fileResource, files)))
                .collect(Collectors.groupingBy(
                        e -> e.getKey().getExtension().getExtension().getId(),
                        Collectors.mapping(Map.Entry::getValue, Collectors.flatMapping(List::stream, Collectors.toList()))
                ));
    }

    private List<Download> toDownloads(FileResource fileResource, Map<String, List<LocalDateTime>> files) {
        return files.get(fileResource.getName().toUpperCase()).stream()
                .map(time -> {
                    var download = new Download();
                    download.setAmount(1);
                    download.setTimestamp(time);
                    download.setFileResourceId(fileResource.getId());
                    return download;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void increaseDownloadCounts(Map<Long, List<Download>> extensionDownloads) {
        repositories.findExtensions(extensionDownloads.keySet()).forEach(extension -> {
            var downloads = extensionDownloads.get(extension.getId());
            downloadCounts.increaseDownloadCount(extension, downloads);
            logger.info("increased downloads for {}.{} by {}", extension.getNamespace().getName(), extension.getName(), downloads.size());
        });
    }

    public List<String> processedItems(List<String> blobNames) {
        return repositories.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(blobNames);
    }
}
