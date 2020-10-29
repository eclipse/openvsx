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

import javax.persistence.EntityManager;

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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ChangelogInitializer {

    protected final Logger logger = LoggerFactory.getLogger(ChangelogInitializer.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    TransactionTemplate transactions;

    @EventListener
    @Transactional(readOnly = true)
    public void initExtensionChangelogs(ApplicationStartedEvent event) {
        var count = new int[1];
        repositories.findAllExtensionVersions().forEach(extVersion -> {
            var needsChangelog = repositories.findFileByType(extVersion, FileResource.CHANGELOG) == null;
            if (!needsChangelog)
                return;
            var binary = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
            if (binary == null)
                return;
            try {
                var processor = new ExtensionProcessor(binary.getContent());
                var changelog = processor.getChangelog(extVersion);
                if (changelog != null) {
                    // TODO support external storage types
                    changelog.setStorageType(FileResource.STORAGE_DB);
                    transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transactions.<Void>execute(status -> {
                        entityManager.persist(changelog);
                        return null;
                    });
                    count[0]++;
                }
            } catch (ErrorResultException exc) {
                var extension = extVersion.getExtension();
                logger.error("Failed to create changelog resource for "
                        + extension.getNamespace().getName() + "." + extension.getName()
                        + " v" + extVersion.getVersion(), exc);
            }
            System.gc();
        });

        if (count[0] > 0)
            logger.info("Initialized changelog resource for " + count[0] + " extensions.");
    }

}