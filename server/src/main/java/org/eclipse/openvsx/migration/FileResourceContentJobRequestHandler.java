/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.nio.file.Files;

public class FileResourceContentJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    private final MigrationService migrations;

    public FileResourceContentJobRequestHandler(MigrationService migrations) {
        this.migrations = migrations;
    }

    @Override
    public void run(MigrationJobRequest migrationJobRequest) throws Exception {
        var resource = migrations.getResource(migrationJobRequest);
        try (var content = new TempFile("file_resource", null)) {
            content.setResource(resource);
            Files.write(content.getPath(), resource.getContent());
            migrations.uploadFileResource(content);
        }

        resource.clearContent();
        migrations.updateResource(resource);
    }
}
