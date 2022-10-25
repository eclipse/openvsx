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

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    DownloadCountService downloadCountService;

    @Transactional
    public void persistProcessedItem(String name, LocalDateTime processedOn, int executionTime, boolean success) {
        var processedItem = new AzureDownloadCountProcessedItem();
        processedItem.setName(name);
        processedItem.setProcessedOn(processedOn);
        processedItem.setExecutionTime(executionTime);
        processedItem.setSuccess(success);
        entityManager.persist(processedItem);
    }

    @Transactional
    public void processDownloadCounts(Map<String, List<LocalDateTime>> files) {
        var fileResources = repositories.findDownloadsByStorageTypeAndName(STORAGE_AZURE, files.keySet());
        var extensions = fileResources.stream()
                .map(FileResource::getExtension)
                .map(ExtensionVersion::getExtension)
                .collect(Collectors.toMap(e -> e.getId(), e -> e, (e1, e2) -> e1));

        var extensionDownloads = extensions.keySet().stream()
                .collect(Collectors.toMap(id -> id, id -> new ArrayList<Download>()));

        for (var fileResource : fileResources) {
            var extension = fileResource.getExtension().getExtension();
            var downloads = extensionDownloads.get(extension.getId());
            files.get(fileResource.getName().toUpperCase()).stream()
                    .map(time -> {
                        var download = new Download();
                        download.setAmount(1);
                        download.setTimestamp(time);
                        download.setFileResourceId(fileResource.getId());
                        return download;
                    }).forEach(downloads::add);
        }

        extensionDownloads.forEach((id, downloads) -> downloadCountService.increaseDownloadCount(extensions.get(id), downloads));
    }

    public List<String> processedItems(List<String> blobNames) {
        return repositories.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(blobNames);
    }
}
