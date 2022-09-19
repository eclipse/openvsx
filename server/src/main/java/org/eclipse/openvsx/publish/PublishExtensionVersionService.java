/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.LICENSE;

@Component
public class PublishExtensionVersionService {

    @Autowired
    ExtensionService extensions;

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    StorageUtilService storageUtil;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorModeEnabled;

    @Transactional
    public void publish(String namespaceName, String extensionName, String targetPlatform, String version) throws IOException {
        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespaceName);
        var resources = repositories.findFiles(extVersion);
        var download = resources.stream().filter(r -> r.getType().equals(DOWNLOAD)).findFirst().get();
        try(
                var input = new ByteArrayInputStream(download.getContent());
                var processor = new ExtensionProcessor(input)
        ) {
            var otherResources = processor.getResources(extVersion, List.of(DOWNLOAD, LICENSE));
            otherResources.forEach(fr -> entityManager.persist(fr));
            resources = resources.and(otherResources);
        }

        // Store file resources in the DB or external storage
        for(var resource : resources) {
            if (storageUtil.shouldStoreExternally(resource)) {
                storageUtil.uploadFile(resource);
                // Don't store the binary content in the DB - it's now stored externally
                resource.setContent(null);
            } else {
                resource.setStorageType(FileResource.STORAGE_DB);
            }
        }

        // When mirror mode is enabled all extension versions are activated at once in MirrorActivateExtensionJob
        if(!mirrorModeEnabled) {
            // Update whether extension is active, the search index and evict cache
            extVersion.setActive(true);
            extensions.updateExtension(extVersion.getExtension());
        }
    }
}
