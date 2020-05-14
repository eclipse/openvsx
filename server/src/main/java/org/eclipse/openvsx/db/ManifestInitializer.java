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

import java.io.ByteArrayInputStream;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ManifestInitializer {

    protected final Logger logger = LoggerFactory.getLogger(LicenseInitializer.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @EventListener
    @Transactional
    public void initExtensionManifests(ApplicationStartedEvent event) {
        var count = new int[2];
        repositories.findAllExtensionVersions().forEach(extVersion -> {
            var needsManifest = repositories.findFile(extVersion, FileResource.MANIFEST) == null;
            var needsEngines = extVersion.getEngines() == null || extVersion.getEngines().isEmpty();
            if (needsManifest || needsEngines) {
                var binary = repositories.findFile(extVersion, FileResource.DOWNLOAD);
                if (binary != null) {
                    try {
                        var processor = new ExtensionProcessor(new ByteArrayInputStream(binary.getContent()));
                        // DB 1.2: Add resource for missing manifest (package.json)
                        if (needsManifest) {
                            var manifest = processor.getManifest(extVersion);
                            if (manifest != null) {
                                entityManager.persist(manifest);
                                count[0]++;
                            }
                        }
                        // DB 1.3: Load missing attributes from package.json
                        if (needsEngines) {
                            var metadata = processor.getMetadata();
                            if (metadata.getEngines() != null) {
                                extVersion.setEngines(metadata.getEngines());
                                count[1]++;
                            }
                        }
                    } catch (ErrorResultException exc) {
                        var extension = extVersion.getExtension();
                        logger.error("Failed to create manifest resource for "
                                + extension.getNamespace().getName() + "." + extension.getName()
                                + " v" + extVersion.getVersion(), exc);
                    }
                }
            }
        });

        if (count[0] > 0)
            logger.info("Initialized manifest resource for " + count[0] + " extensions.");
        if (count[1] > 0)
            logger.info("Initialized engines field for " + count[1] + " extensions.");
    }

}