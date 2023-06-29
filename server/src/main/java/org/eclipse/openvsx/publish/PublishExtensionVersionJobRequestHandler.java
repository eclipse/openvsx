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
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.migration.MigrationService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.function.Consumer;

@Component
public class PublishExtensionVersionJobRequestHandler implements JobRequestHandler<PublishExtensionVersionJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionJobRequestHandler.class);

    @Autowired
    ExtensionService extensions;

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    @Autowired
    PublishExtensionVersionService service;

    @Autowired
    MigrationService migrations;

    @Override
    @Job(name = "Process extension version file resources", retries = 10)
    public void run(PublishExtensionVersionJobRequest jobRequest) throws Exception {
        var download = service.getDownload(jobRequest);
        var extVersion = download.getExtension();

        // Delete file resources in case this job is retried
        service.deleteFileResources(extVersion);
        try (var extensionFile = new TempFile("extension_", ".vsix")) {
            Files.write(extensionFile.getPath(), migrations.getContent(download));
            try (var processor = new ExtensionProcessor(extensionFile)) {
                service.storeDownload(download);
                Consumer<FileResource> consumer = resource -> {
                    service.storeResource(resource);
                    service.persistResource(resource);
                };

                if (integrityService.isEnabled()) {
                    var keyPair = extVersion.getSignatureKeyPair();
                    if (keyPair != null) {
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
        }

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion, extensions);
    }
}
