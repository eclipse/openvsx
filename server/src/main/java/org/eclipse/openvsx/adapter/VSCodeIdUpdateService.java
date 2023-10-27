/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.BuiltInExtensionUtil;
import org.eclipse.openvsx.util.NamingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class VSCodeIdUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSCodeIdUpdateService.class);
    private static final Semaphore LOCK = new Semaphore(1);

    @Autowired
    RepositoryService repositories;

    @Autowired
    VSCodeIdService service;

    public void update(String namespaceName, String extensionName) throws InterruptedException {
        var acquired = LOCK.tryAcquire(15, TimeUnit.SECONDS);
        if(!acquired) {
            throw new RuntimeException("Failed to update public id for " + NamingUtil.toExtensionId(namespaceName, extensionName));
        }
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            LOGGER.debug("SKIP BUILT-IN EXTENSION {}", NamingUtil.toExtensionId(namespaceName, extensionName));
            return;
        }

        var extension = repositories.findPublicId(namespaceName, extensionName);
        var extensionUpdates = new HashMap<Long, String>();
        updateExtensionPublicId(extension, extensionUpdates);
        if(!extensionUpdates.isEmpty()) {
            repositories.updateExtensionPublicIds(extensionUpdates);
        }

        var namespaceUpdates = new HashMap<Long, String>();
        updateNamespacePublicId(extension, namespaceUpdates);
        if(!namespaceUpdates.isEmpty()) {
            repositories.updateNamespacePublicIds(namespaceUpdates);
        }
        LOCK.release();
    }

    private void updateExtensionPublicId(Extension extension, Map<Long, String> updates) {
        LOGGER.debug("updateExtensionPublicId: {}", NamingUtil.toExtensionId(extension));
        service.getUpstreamPublicIds(extension);
        if(extension.getPublicId() == null) {
            var publicId = "";
            do {
                publicId = UUID.randomUUID().toString();
                LOGGER.debug("RANDOM EXTENSION PUBLIC ID: {}", publicId);
            } while(updates.containsValue(publicId) || repositories.extensionPublicIdExists(publicId));
            LOGGER.debug("RANDOM PUT UPDATE: {} - {}", extension.getId(), publicId);
            updates.put(extension.getId(), publicId);
        } else {
            LOGGER.debug("UPSTREAM PUT UPDATE: {} - {}", extension.getId(), extension.getPublicId());
            updates.put(extension.getId(), extension.getPublicId());
            var duplicatePublicId = repositories.findPublicId(extension.getPublicId());
            if(duplicatePublicId != null) {
                updateExtensionPublicId(duplicatePublicId, updates);
            }
        }
    }

    private void updateNamespacePublicId(Extension extension, Map<Long, String> updates) {
        LOGGER.debug("updateNamespacePublicId: {}", extension.getNamespace().getName());
        service.getUpstreamPublicIds(extension);
        var namespace = extension.getNamespace();
        if(namespace.getPublicId() == null) {
            var publicId = "";
            do {
                publicId = UUID.randomUUID().toString();
                LOGGER.debug("RANDOM NAMESPACE PUBLIC ID: {}", publicId);
            } while(updates.containsValue(publicId) || repositories.namespacePublicIdExists(publicId));
            LOGGER.debug("RANDOM PUT UPDATE: {} - {}", namespace.getId(), publicId);
            updates.put(namespace.getId(), publicId);
        } else {
            LOGGER.debug("UPSTREAM PUT UPDATE: {} - {}", namespace.getId(), namespace.getPublicId());
            updates.put(namespace.getId(), namespace.getPublicId());
            var duplicatePublicId = repositories.findNamespacePublicId(namespace.getPublicId());
            if(duplicatePublicId != null) {
                updateNamespacePublicId(duplicatePublicId, updates);
            }
        }
    }

    public void updateAll() throws InterruptedException {
        LOCK.acquire();
        LOGGER.debug("DAILY UPDATE ALL");
        var extensions = repositories.findAllPublicIds();
        var extensionPublicIdsMap = extensions.stream()
                .filter(e -> StringUtils.isNotEmpty(e.getPublicId()))
                .collect(Collectors.toMap(e -> e.getId(), e -> e.getPublicId()));
        var namespacePublicIdsMap = extensions.stream()
                .map(e -> e.getNamespace())
                .filter(n -> StringUtils.isNotEmpty(n.getPublicId()))
                .collect(Collectors.toMap(n -> n.getId(), n -> n.getPublicId(), (id1, id2) -> id1));

        var upstreamExtensionPublicIds = new HashMap<Long, String>();
        var upstreamNamespacePublicIds = new HashMap<Long, String>();
        for(var extension : extensions) {
            if(BuiltInExtensionUtil.isBuiltIn(extension)) {
                LOGGER.trace("SKIP BUILT-IN EXTENSION {}", NamingUtil.toExtensionId(extension));
                continue;
            }

            LOGGER.trace("GET UPSTREAM PUBLIC ID: {} | {}", extension.getId(), NamingUtil.toExtensionId(extension));
            service.getUpstreamPublicIds(extension);
            if(upstreamExtensionPublicIds.get(extension.getId()) == null) {
                LOGGER.trace("ADD EXTENSION PUBLIC ID: {} - {}", extension.getId(), extension.getPublicId());
                upstreamExtensionPublicIds.put(extension.getId(), extension.getPublicId());
            }

            var namespace = extension.getNamespace();
            if(upstreamNamespacePublicIds.get(namespace.getId()) == null) {
                LOGGER.trace("ADD NAMESPACE PUBLIC ID: {} - {}", namespace.getId(), namespace.getPublicId());
                upstreamNamespacePublicIds.put(namespace.getId(), namespace.getPublicId());
            }
        }

        var changedExtensionPublicIds = getChangedPublicIds(upstreamExtensionPublicIds, extensionPublicIdsMap);
        LOGGER.debug("UPSTREAM EXTENSIONS: {}", upstreamExtensionPublicIds.size());
        LOGGER.debug("CHANGED EXTENSIONS: {}", changedExtensionPublicIds.size());
        if(!changedExtensionPublicIds.isEmpty()) {
            LOGGER.debug("CHANGED EXTENSION PUBLIC IDS");
            for(var entry : changedExtensionPublicIds.entrySet()) {
                LOGGER.debug("{}: {}", entry.getKey(), entry.getValue());
            }

            repositories.updateExtensionPublicIds(changedExtensionPublicIds);
        }

        var changedNamespacePublicIds = getChangedPublicIds(upstreamNamespacePublicIds, namespacePublicIdsMap);
        LOGGER.debug("UPSTREAM NAMESPACES: {}", upstreamNamespacePublicIds.size());
        LOGGER.debug("CHANGED NAMESPACES: {}", changedNamespacePublicIds.size());
        if(!changedNamespacePublicIds.isEmpty()) {
            LOGGER.debug("CHANGED NAMESPACE PUBLIC IDS");
            for(var entry : changedNamespacePublicIds.entrySet()) {
                LOGGER.debug("{}: {}", entry.getKey(), entry.getValue());
            }

            repositories.updateNamespacePublicIds(changedNamespacePublicIds);
        }

        LOCK.release();
    }

    private Map<Long, String> getChangedPublicIds(Map<Long, String> upstreamPublicIds, Map<Long, String> currentPublicIds) {
        var changedPublicIds = new HashMap<Long, String>();
        upstreamPublicIds.entrySet().stream()
                .filter(e -> !Objects.equals(currentPublicIds.get(e.getKey()), e.getValue()))
                .forEach(e -> changedPublicIds.put(e.getKey(), e.getValue()));

        if(!changedPublicIds.isEmpty()) {
            var newPublicIds = new HashSet<>(upstreamPublicIds.values());
            updatePublicIdNulls(changedPublicIds, newPublicIds, currentPublicIds);
        }

        return changedPublicIds;
    }

    private void updatePublicIdNulls(Map<Long, String> changedPublicIds, Set<String> newPublicIds, Map<Long, String> publicIdMap) {
        // remove unchanged random public ids
        changedPublicIds.entrySet().removeIf((e) -> {
            var publicId = e.getValue() == null ? publicIdMap.get(e.getKey()) : null;
            var remove = publicId != null && !newPublicIds.contains(publicId);
            if(remove) {
                newPublicIds.add(publicId);
            }

            return remove;
        });

        // put random UUIDs where upstream public id is missing
        for(var key : changedPublicIds.keySet()) {
            if(changedPublicIds.get(key) != null) {
                continue;
            }

            String publicId = null;
            while(newPublicIds.contains(publicId)) {
                publicId = UUID.randomUUID().toString();
                LOGGER.debug("NEW PUBLIC ID - {}: '{}'", key, publicId);
            }

            changedPublicIds.put(key, publicId);
            newPublicIds.add(publicId);
        }
    }
}
