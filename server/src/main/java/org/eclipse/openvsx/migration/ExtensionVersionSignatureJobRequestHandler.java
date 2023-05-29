/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class ExtensionVersionSignatureJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtensionVersionSignatureJobRequestHandler.class));

    @Autowired
    RepositoryService repositories;

    @Autowired
    CacheService cache;

    @Autowired
    MigrationService migrations;

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    @Override
    @Job(name = "Generate signature for extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var extVersion = migrations.getExtension(jobRequest.getEntityId());
        if(extVersion == null) {
            return;
        }

        logger.info("Generating signature for: {}", NamingUtil.toLogFormat(extVersion));

        var existingSignature = migrations.getFileResource(extVersion, FileResource.DOWNLOAD_SIG);
        if(existingSignature != null) {
            migrations.removeFile(existingSignature);
            migrations.deleteFileResource(existingSignature);
        }

        var entry = migrations.getDownload(extVersion);
        try(var extensionFile = migrations.getExtensionFile(entry)) {
            var download = entry.getKey();
            var keyPair = repositories.findActiveKeyPair();
            var signature = integrityService.generateSignature(download, extensionFile, keyPair);
            signature.setStorageType(download.getStorageType());
            integrityService.setSignatureKeyPair(extVersion, keyPair);

            var extension = extVersion.getExtension();
            cache.evictExtensionJsons(extVersion);
            cache.evictLatestExtensionVersion(extension);
            cache.evictNamespaceDetails(extension);

            migrations.uploadFileResource(signature);
            migrations.persistFileResource(signature);
        }
    }
}
