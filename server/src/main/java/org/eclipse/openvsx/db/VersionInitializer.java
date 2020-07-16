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

import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.SemanticVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VersionInitializer {

    protected final Logger logger = LoggerFactory.getLogger(VersionInitializer.class);

    @Autowired
    RepositoryService repositories;

    @EventListener
    @Transactional
    public void initExtensionLicenses(ApplicationStartedEvent event) {
        var count = new int[2];
        repositories.findAllExtensions().forEach(extension -> {
            ExtensionVersion latest = null;
            SemanticVersion latestVer = null;
            ExtensionVersion preview = null;
            SemanticVersion previewVer = null;
            for (var extVersion : extension.getVersions()) {
                var ver = extVersion.getSemanticVersion();
                if (extVersion.isPreview() && isGreater(extVersion, ver, preview, previewVer)) {
                    preview = extVersion;
                    previewVer = ver;
                } else if (!extVersion.isPreview() && isGreater(extVersion, ver, latest, latestVer)) {
                    latest = extVersion;
                    latestVer = ver;
                }
            }
            if (latest != null && latest != extension.getLatest()) {
                extension.setLatest(latest);
                count[0]++;
            }
            if (preview != null && preview != extension.getPreview()) {
                extension.setPreview(preview);
                count[1]++;
            }
        });

        if (count[0] > 0)
            logger.info("Updated latest version for " + count[0] + " extensions.");
        if (count[1] > 0)
            logger.info("Updated preview version for " + count[1] + " extensions.");
    }

    private boolean isGreater(ExtensionVersion ev1, SemanticVersion sv1, ExtensionVersion ev2, SemanticVersion sv2) {
        if (ev2 == null)
            return true;
        var cmp = sv1.compareTo(sv2);
        return cmp > 0 || cmp == 0 && ev1.getTimestamp().isAfter(ev2.getTimestamp());
    }

}