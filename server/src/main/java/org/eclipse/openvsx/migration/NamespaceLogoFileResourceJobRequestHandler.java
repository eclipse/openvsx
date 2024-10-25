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

import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
public class NamespaceLogoFileResourceJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    private final NamespaceLogoFileResourceService service;
    private final StorageUtilService storageUtil;

    public NamespaceLogoFileResourceJobRequestHandler(NamespaceLogoFileResourceService service, StorageUtilService storageUtil) {
        this.service = service;
        this.storageUtil = storageUtil;
    }

    @Override
    @Job(name = "Use FileResource for Namespace logo", retries = 3)
    public void run(MigrationJobRequest migrationJobRequest) throws Exception {
        var id = migrationJobRequest.getEntityId();
        var namespace = service.getNamespace(id);
        try (var content = new TempFile("namespace_logo", "")) {
            content.setNamespace(namespace);
            Files.write(content.getPath(), namespace.getLogoBytes());
            storageUtil.uploadNamespaceLogo(content);
        }

        namespace.clearLogoBytes();
        service.updateNamespace(namespace);
    }
}
