/********************************************************************************
 * Copyright (c) 2024 STMicroelectronics and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.migration;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CheckPotentiallyMaliciousExtensionVersionsService {

    protected final Logger logger = LoggerFactory.getLogger(CheckPotentiallyMaliciousExtensionVersionsService.class);

    private final EntityManager entityManager;

    public CheckPotentiallyMaliciousExtensionVersionsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void checkPotentiallyMaliciousExtensionVersion(ExtensionVersion extVersion, TempFile extensionFile) {
        try(var extProcessor = new ExtensionProcessor(extensionFile)) {
            boolean isMalicious = extProcessor.isPotentiallyMalicious();
            extVersion.setPotentiallyMalicious(isMalicious);
            if (isMalicious && logger.isWarnEnabled()) {
                logger.warn("Extension version is potentially malicious: {}", NamingUtil.toLogFormat(extVersion));
            }
        }
        entityManager.merge(extVersion);
    }
}
