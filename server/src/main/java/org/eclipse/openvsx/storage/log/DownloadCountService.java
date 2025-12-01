/********************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage.log;

import org.eclipse.openvsx.entities.FileResource;
import org.springframework.stereotype.Component;

@Component
public class DownloadCountService {

    private final AwsDownloadCountService awsDownloadCountService;
    private final AzureDownloadCountService azureDownloadCountService;

    public DownloadCountService(
            AwsDownloadCountService awsDownloadCountService,
            AzureDownloadCountService azureDownloadCountService
    ) {
        this.awsDownloadCountService = awsDownloadCountService;
        this.azureDownloadCountService = azureDownloadCountService;
    }

    public boolean isEnabled(FileResource resource) {
        return switch (resource.getStorageType()) {
            case FileResource.STORAGE_AWS -> awsDownloadCountService.isEnabled();
            case FileResource.STORAGE_AZURE -> azureDownloadCountService.isEnabled();
            default -> false;
        };
    }
}