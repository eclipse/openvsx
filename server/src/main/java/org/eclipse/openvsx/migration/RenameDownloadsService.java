/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.FileResource;
import org.springframework.stereotype.Component;

@Component
public class RenameDownloadsService {

    private final EntityManager entityManager;

    public RenameDownloadsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public FileResource cloneResource(FileResource resource, String name) {
        resource = entityManager.merge(resource);
        var clone = new FileResource();
        clone.setName(name);
        clone.setStorageType(resource.getStorageType());
        clone.setType(resource.getType());
        clone.setExtension(resource.getExtension());
        return clone;
    }

    @Transactional
    public void updateResource(FileResource resource) {
        entityManager.merge(resource);
    }
}
