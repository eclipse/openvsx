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

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.SetPreReleaseMigrationItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.IStorageService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.util.Map;

@Component
public class SetPreReleaseJobRequestHandler implements JobRequestHandler<SetPreReleaseJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtractResourcesJobRequestHandler.class));

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Autowired
    RestTemplate restTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Job(name = "Set pre-release and preview for published extensions", retries = 3)
    public void run(SetPreReleaseJobRequest jobRequest) throws Exception {
        var item = entityManager.find(SetPreReleaseMigrationItem.class, jobRequest.getItemId());
        var extension = item.getExtension();
        logger.info("Setting pre-release for: {}.{}", extension.getNamespace().getName(), extension.getName());
        for(var extVersion : extension.getVersions()) {
            try(var input = new ByteArrayInputStream(getContent(extVersion))) {
                try(var extProcessor = new ExtensionProcessor(input)) {
                    extVersion.setPreRelease(extProcessor.isPreRelease());
                    extVersion.setPreview(extProcessor.isPreview());
                }
            }
        }
    }

    private byte[] getContent(ExtensionVersion extVersion) {
        var download = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        if(download.getStorageType().equals(FileResource.STORAGE_DB)) {
            return download.getContent();
        } else {
            var storage = getStorage(download);
            var uri = storage.getLocation(download);
            return restTemplate.getForObject(uri, byte[].class);
        }
    }

    private IStorageService getStorage(FileResource resource) {
        var storages = Map.of(
                FileResource.STORAGE_AZURE, azureStorage,
                FileResource.STORAGE_GOOGLE, googleStorage
        );

        return storages.get(resource.getStorageType());
    }
}
