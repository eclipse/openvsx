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
import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Component
public class RenameDownloadsService {

    @Autowired
    EntityManager entityManager;

    public String getNewBinaryName(FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        var resourceName = namespace.getName() + "." + extension.getName() + "-" + extVersion.getVersion();
        if(!TargetPlatform.isUniversal(extVersion.getTargetPlatform())) {
            resourceName += "@" + extVersion.getTargetPlatform();
        }

        resourceName += ".vsix";
        return resourceName;
    }

    @Transactional
    public byte[] getContent(FileResource download) {
        download = entityManager.merge(download);
        return download.getStorageType().equals(FileResource.STORAGE_DB) ? download.getContent() : null;
    }

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

    public FileResource getResource(MigrationJobRequest jobRequest) {
        return entityManager.find(FileResource.class, jobRequest.getEntityId());
    }

    @Transactional
    public void updateResource(FileResource resource) {
        entityManager.merge(resource);
    }
}
