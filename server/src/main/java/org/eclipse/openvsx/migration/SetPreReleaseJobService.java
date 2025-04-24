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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetPreReleaseJobService {

    private final EntityManager entityManager;

    public SetPreReleaseJobService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public List<ExtensionVersion> getExtensionVersions(MigrationJobRequest jobRequest, Logger logger) {
        var extension = entityManager.find(Extension.class, jobRequest.getEntityId());
        if(logger.isInfoEnabled()) {
            logger.info("Setting pre-release for: {}", NamingUtil.toExtensionId(extension));
        }

        return extension.getVersions();
    }

    @Transactional
    public void updatePreviewAndPreRelease(ExtensionVersion extVersion, TempFile extensionFile) {
        try(var extProcessor = new ExtensionProcessor(extensionFile)) {
            extVersion.setPreRelease(extProcessor.isPreRelease());
            extVersion.setPreview(extProcessor.isPreview());
        }

        entityManager.merge(extVersion);
    }
}
