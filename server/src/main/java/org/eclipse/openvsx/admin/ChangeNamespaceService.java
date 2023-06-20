/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.admin;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChangeNamespaceService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    CacheService cache;

    @Autowired
    SearchUtilService search;

    @Transactional
    public void changeNamespaceInDatabase(
            Namespace newNamespace,
            Namespace oldNamespace,
            List<FileResource> updatedResources,
            boolean createNewNamespace,
            boolean removeOldNamespace
    ) {
        var extensions = repositories.findExtensions(oldNamespace);
        for(var extension : extensions) {
            cache.evictExtensionJsons(extension);
            cache.evictLatestExtensionVersion(extension);
        }

        if(createNewNamespace) {
            entityManager.persist(newNamespace);
        } else {
            newNamespace = entityManager.merge(newNamespace);
        }

        changeExtensionNamespace(extensions, newNamespace);
        changeMembershipNamespace(oldNamespace, newNamespace, removeOldNamespace);
        updatedResources.forEach(entityManager::merge);

        if(removeOldNamespace) {
            oldNamespace = entityManager.merge(oldNamespace);
            entityManager.remove(oldNamespace);
        }

        search.updateSearchEntries(extensions.toList());
    }

    private void changeExtensionNamespace(Streamable<Extension> extensions, Namespace newNamespace) {
        for(var extension : extensions) {
            extension = entityManager.merge(extension);
            extension.setNamespace(newNamespace);
        }
    }

    private void changeMembershipNamespace(Namespace oldNamespace, Namespace newNamespace, boolean removeOldNamespace) {
        var oldMemberships = repositories.findMemberships(oldNamespace).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m));
        var newMemberships = repositories.findMemberships(newNamespace).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), m -> m));

        for(var entry : oldMemberships.entrySet()) {
            if(!newMemberships.containsKey(entry.getKey())) {
                entry.getValue().setNamespace(newNamespace);
            } else if (removeOldNamespace) {
                entityManager.remove(entry.getValue());
            }
        }
    }
}
