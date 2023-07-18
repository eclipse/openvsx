/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.migration.MigrationService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.function.Consumer;

@Component
public class PublishExtensionVersionJob implements JobRequestHandler<PublishExtensionVersionJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionJob.class);

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    @Autowired
    PublishExtensionVersionJobService service;

    @Autowired
    MigrationService migrations;

    @Override
    public void run(PublishExtensionVersionJobRequest jobRequest) throws Exception {
        var download = service.getFileResource(jobRequest.getDownloadId());
        var extVersion = download.getExtension();
        logger.info("Processing files for {}", NamingUtil.toLogFormat(extVersion));

        // Delete file resources in case job is retried
        service.deleteFileResources(extVersion);

        var content = migrations.getContent(download);
        var extensionFile = migrations.getExtensionFile(new AbstractMap.SimpleEntry<>(download, content));
        try(var processor = new ExtensionProcessor(extensionFile)) {
            Consumer<FileResource> consumer = resource -> {
                service.storeResource(resource);
                service.persistResource(resource);
            };

            if(integrityService.isEnabled()) {
                var keyPair = extVersion.getSignatureKeyPair();
                if(keyPair != null) {
                    var signature = integrityService.generateSignature(download, extensionFile, keyPair);
                    consumer.accept(signature);
                } else {
                    // Can happen when GenerateKeyPairJobRequestHandler hasn't run yet and there is no active SignatureKeyPair.
                    // This extension version should be assigned a SignatureKeyPair and a signature FileResource should be created
                    // by the ExtensionVersionSignatureJobRequestHandler migration.
                    logger.warn("Integrity service is enabled, but {} did not have an active key pair", NamingUtil.toLogFormat(extVersion));
                }
            }

            processor.processEachResource(extVersion, consumer);
            processor.getFileResources(extVersion).forEach(consumer);
            consumer.accept(processor.generateSha256Checksum(extVersion));
        }

        service.storeDownload(download);

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion);
        if(!download.getStorageType().equals(FileResource.STORAGE_DB)) {
            // Don't store the binary content in the DB - it's now stored externally
            download.setContent(null);
        }

        service.updateResource(download);
        logger.info("Published {}", NamingUtil.toLogFormat(extVersion));
    }
}
