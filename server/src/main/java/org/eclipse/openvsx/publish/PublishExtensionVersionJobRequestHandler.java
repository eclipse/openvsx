/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.LICENSE;

@Component
public class PublishExtensionVersionJobRequestHandler implements JobRequestHandler<PublishExtensionVersionJobRequest> {

    @Autowired
    ExtensionService extensions;

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    StorageUtilService storageUtil;

    @Override
    @Transactional
    public void run(PublishExtensionVersionJobRequest jobRequest) throws Exception {
        var extVersion = repositories.findVersion(jobRequest.getVersion(), jobRequest.getTargetPlatform(),
                jobRequest.getExtensionName(), jobRequest.getNamespaceName());

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

        // Update whether extension is active, the search index and evict cache
        extVersion.setActive(true);
        extensions.updateExtension(extVersion.getExtension());
    }
}
