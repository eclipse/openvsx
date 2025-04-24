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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class VSCodeIdUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSCodeIdUpdateService.class);

    private final RepositoryService repositories;
    private final VSCodeIdService service;

    public VSCodeIdUpdateService(RepositoryService repositories, VSCodeIdService service) {
        this.repositories = repositories;
        this.service = service;
    }

    public void update(String namespaceName, String extensionName) throws InterruptedException {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("SKIP BUILT-IN EXTENSION {}", NamingUtil.toExtensionId(namespaceName, extensionName));
            }
            return;
        }

        var extension = repositories.findPublicId(namespaceName, extensionName);
        var extensionUpdates = new HashMap<Long, String>();
        updateExtensionPublicId(extension, extensionUpdates, false);
        if(!extensionUpdates.isEmpty()) {
            repositories.updateExtensionPublicIds(extensionUpdates);
        }

        var namespaceUpdates = new HashMap<Long, String>();
        updateNamespacePublicId(extension, namespaceUpdates, false);
        if(!namespaceUpdates.isEmpty()) {
            repositories.updateNamespacePublicIds(namespaceUpdates);
        }
    }

    private void updateExtensionPublicId(Extension extension, Map<Long, String> updates, boolean mustUpdate) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("updateExtensionPublicId: {}", NamingUtil.toExtensionId(extension));
        }

        var oldPublicId = extension.getPublicId();
        var newPublicId = service.getUpstreamPublicIds(extension).extension();
        if(newPublicId == null || (mustUpdate && newPublicId.equals(oldPublicId))) {
            do {
                newPublicId = service.getRandomPublicId();
                LOGGER.debug("RANDOM EXTENSION PUBLIC ID: {}", newPublicId);
            } while(updates.containsValue(newPublicId) || repositories.extensionPublicIdExists(newPublicId));
            LOGGER.debug("RANDOM PUT UPDATE: {} - {}", extension.getId(), newPublicId);
            updates.put(extension.getId(), newPublicId);
        } else if (!newPublicId.equals(oldPublicId)) {
            LOGGER.debug("UPSTREAM PUT UPDATE: {} - {}", extension.getId(), newPublicId);
            updates.put(extension.getId(), newPublicId);
            var duplicatePublicId = repositories.findPublicId(newPublicId);
            if(duplicatePublicId != null) {
                updateExtensionPublicId(duplicatePublicId, updates, true);
            }
        }
    }

    private void updateNamespacePublicId(Extension extension, Map<Long, String> updates, boolean mustUpdate) {
        LOGGER.debug("updateNamespacePublicId: {}", extension.getNamespace().getName());
        var oldPublicId = extension.getNamespace().getPublicId();
        var newPublicId = service.getUpstreamPublicIds(extension).namespace();
        var id = extension.getNamespace().getId();
        if(newPublicId == null || (mustUpdate && newPublicId.equals(oldPublicId))) {
            do {
                newPublicId = service.getRandomPublicId();
                LOGGER.debug("RANDOM NAMESPACE PUBLIC ID: {}", newPublicId);
            } while(updates.containsValue(newPublicId) || repositories.namespacePublicIdExists(newPublicId));
            LOGGER.debug("RANDOM PUT UPDATE: {} - {}", id, newPublicId);
            updates.put(id, newPublicId);
        } else if(!newPublicId.equals(oldPublicId)) {
            LOGGER.debug("UPSTREAM PUT UPDATE: {} - {}", id, newPublicId);
            updates.put(id, newPublicId);
            var duplicatePublicId = repositories.findNamespacePublicId(newPublicId);
            if(duplicatePublicId != null) {
                updateNamespacePublicId(duplicatePublicId, updates, true);
            }
        }
    }

    public void updateAll() throws InterruptedException {
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
                if(LOGGER.isTraceEnabled()) {
                    LOGGER.trace("SKIP BUILT-IN EXTENSION {}", NamingUtil.toExtensionId(extension));
                }
                continue;
            }
            if(LOGGER.isTraceEnabled()) {
                LOGGER.trace("GET UPSTREAM PUBLIC ID: {} | {}", extension.getId(), NamingUtil.toExtensionId(extension));
            }

            var publicIds = service.getUpstreamPublicIds(extension);
            if(upstreamExtensionPublicIds.get(extension.getId()) == null) {
                LOGGER.trace("ADD EXTENSION PUBLIC ID: {} - {}", extension.getId(), publicIds.extension());
                upstreamExtensionPublicIds.put(extension.getId(), publicIds.extension());
            }

            var namespace = extension.getNamespace();
            if(upstreamNamespacePublicIds.get(namespace.getId()) == null) {
                LOGGER.trace("ADD NAMESPACE PUBLIC ID: {} - {}", namespace.getId(), publicIds.namespace());
                upstreamNamespacePublicIds.put(namespace.getId(), publicIds.namespace());
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
        changedPublicIds.entrySet().removeIf(e -> {
            var publicId = e.getValue() == null ? publicIdMap.get(e.getKey()) : null;
            var remove = publicId != null && !newPublicIds.contains(publicId);
            if(remove) {
                newPublicIds.add(publicId);
            }

            return remove;
        });

        // put random public ids where upstream public id is missing
        for(var key : changedPublicIds.keySet()) {
            if(changedPublicIds.get(key) != null) {
                continue;
            }

            String publicId = null;
            while(newPublicIds.contains(publicId)) {
                publicId = service.getRandomPublicId();
                LOGGER.debug("NEW PUBLIC ID - {}: '{}'", key, publicId);
            }

            changedPublicIds.put(key, publicId);
            newPublicIds.add(publicId);
        }
    }
}
