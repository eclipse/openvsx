/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.db;

import java.util.Arrays;

import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.LicenseDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LicenseInitializer {

    protected final Logger logger = LoggerFactory.getLogger(LicenseInitializer.class);

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.licenses.detect:}")
    String[] detectLicenseIds;

    @EventListener
    @Transactional
    public void initExtensionLicenses(ApplicationStartedEvent event) {
        var detection = new LicenseDetection(Arrays.asList(detectLicenseIds));
        var undetected = new int[1];
        repositories.findVersionsByLicense(null).forEach(extVersion -> {
            var license = repositories.findFileByType(extVersion, FileResource.LICENSE);
            if (license != null) {
                var detectedId = detection.detectLicense(license.getContent());
                if (detectedId == null) {
                    undetected[0]++;
                } else {
                    extVersion.setLicense(detectedId);
                    var extension = extVersion.getExtension();
                    logger.info("License of " + extension.getNamespace().getName() + "." + extension.getName()
                            + " v" + extVersion.getVersion() + " set to " + detectedId);
                }
            }
        });

        if (undetected[0] > 0)
            logger.warn("Failed to detect license type for " + undetected[0] + " extensions.");
    }

}