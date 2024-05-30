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
import org.eclipse.openvsx.entities.SignatureKeyPair;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class ExtensionVersionSignatureJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(ExtensionVersionSignatureJobRequestHandler.class));

    private final RepositoryService repositories;
    private final CacheService cache;
    private final MigrationService migrations;
    private final ExtensionVersionIntegrityService integrityService;

    public ExtensionVersionSignatureJobRequestHandler(
            RepositoryService repositories,
            CacheService cache,
            MigrationService migrations,
            ExtensionVersionIntegrityService integrityService
    ) {
        this.repositories = repositories;
        this.cache = cache;
        this.migrations = migrations;
        this.integrityService = integrityService;
    }

    @Override
    @Job(name = "Generate signature for extension version", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var extVersion = migrations.getExtension(jobRequest.getEntityId());
        if(extVersion == null) {
            return;
        }

        var entry = migrations.getDownload(extVersion);
        if(entry == null) {
            return;
        }

        logger.info("Generating signature for: {}", NamingUtil.toLogFormat(extVersion));

        var keyPair = repositories.findActiveKeyPair();
        var signature = createSignature(entry, keyPair);
        if(signature == null) {
            return;
        }

        integrityService.setSignatureKeyPair(extVersion, keyPair);
        var extension = extVersion.getExtension();
        cache.evictExtensionJsons(extVersion);
        cache.evictLatestExtensionVersion(extension);
        cache.evictNamespaceDetails(extension);

        var existingSignature = migrations.getFileResource(extVersion, FileResource.DOWNLOAD_SIG);
        if(existingSignature != null) {
            migrations.removeFile(existingSignature);
            migrations.deleteFileResource(existingSignature);
        }

        migrations.uploadFileResource(signature);
        migrations.persistFileResource(signature);
    }

    private FileResource createSignature(Map.Entry<FileResource, byte[]> entry, SignatureKeyPair keyPair) throws IOException {
        try(var extensionFile = migrations.getExtensionFile(entry)) {
            if(Files.size(extensionFile.getPath()) == 0) {
                return null;
            }

            var download = entry.getKey();
            var signature = integrityService.generateSignature(download, extensionFile, keyPair);
            signature.setStorageType(download.getStorageType());
            return signature;
        }
    }
}
