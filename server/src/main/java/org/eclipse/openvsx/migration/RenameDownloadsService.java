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

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Component
public class RenameDownloadsService {

    @Autowired
    EntityManager entityManager;

    @Transactional
    public FileResource cloneResource(FileResource resource, String name) {
        resource = entityManager.merge(resource);
        var clone = new FileResource();
        clone.setName(name);
        clone.setStorageType(resource.getStorageType());
        clone.setType(resource.getType());
        clone.setExtension(resource.getExtension());
        clone.setContent(resource.getContent());
        return clone;
    }

    @Transactional
    public void updateResource(FileResource resource) {
        entityManager.merge(resource);
    }
}
