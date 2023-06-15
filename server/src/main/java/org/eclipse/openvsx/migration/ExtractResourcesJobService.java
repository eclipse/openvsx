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

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

@Component
public class ExtractResourcesJobService {

    @Autowired
    RepositoryService repositories;

    @Transactional
    public void deleteResources(ExtensionVersion extVersion) {
        repositories.deleteFileResources(extVersion, "resource");
    }

    @Transactional
    public void deleteWebResources(ExtensionVersion extVersion) {
        repositories.deleteFileResources(extVersion, "web-resource");
    }
}
