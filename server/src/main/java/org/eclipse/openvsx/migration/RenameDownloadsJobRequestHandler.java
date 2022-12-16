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

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;

@Component
public class RenameDownloadsJobRequestHandler  implements JobRequestHandler<MigrationJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameDownloadsJobRequestHandler.class);

    @Autowired
    MigrationService migrations;

    @Autowired
    RenameDownloadsService service;

    @Override
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var download = service.getResource(jobRequest);
        var name = service.getNewBinaryName(download);
        if(download.getName().equals(name)) {
            // names are the same, nothing to do
            return;
        }

        LOGGER.info("Renaming download {}", download.getName());
        var content = service.getContent(download);
        var extensionFile = migrations.getExtensionFile(new AbstractMap.SimpleEntry<>(download, content));
        var newDownload = service.cloneResource(download, name);
        migrations.uploadResource(newDownload, extensionFile);
        migrations.deleteResource(download);

        download.setName(name);
        service.updateResource(download);
        LOGGER.info("Updated download name to: {}", name);
    }
}
