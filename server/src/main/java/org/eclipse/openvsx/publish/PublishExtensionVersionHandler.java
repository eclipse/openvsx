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
import org.eclipse.openvsx.entities.FileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.function.Consumer;

@Component
public class PublishExtensionVersionHandler {

    @Autowired
    PublishExtensionVersionService service;

    @Async
    @Retryable
    public void publishAsync(FileResource download, Path extensionFile, ExtensionService extensionService) {
        var extVersion = download.getExtension();
        // Delete file resources in case publishAsync is retried
        service.deleteFileResources(extVersion);
        download.setId(0L);

        service.storeDownload(download, extensionFile);
        service.persistResource(download);
        try(var processor = new ExtensionProcessor(extensionFile)) {
            Consumer<FileResource> consumer = resource -> {
                service.storeResource(resource);
                service.persistResource(resource);
            };

            processor.processEachResource(extVersion, consumer);
            processor.getFileResources(extVersion).forEach(consumer);
        }

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion, extensionService);
    }
}
