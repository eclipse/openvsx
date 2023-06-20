/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;

@Component
public class OrphanNamespaceMigration {

    protected final Logger logger = LoggerFactory.getLogger(OrphanNamespaceMigration.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Transactional
    public void fixOrphanNamespaces() {
        int[] count = new int[3];
        repositories.findOrphanNamespaces().forEach(namespace -> {
            var extensions = repositories.findExtensions(namespace);
            if (extensions.isEmpty()) {
                // The namespace is orphaned and empty - remove it
                entityManager.remove(namespace);
                count[0]++;
            } else {

                // Find all previous contributors
                var contributors = new LinkedHashSet<UserData>();
                for (var extension : extensions) {
                    for (var extVersion : repositories.findActiveVersions(extension)) {
                        if (extVersion.getPublishedWith() != null) {
                            contributors.add(extVersion.getPublishedWith().getUser());
                        }
                    }
                }

                if (contributors.isEmpty()) {
                    // This can happen if all extension versions are inactive
                    count[2]++;
                } else {
                    // Assign explicit memberships to the previous contributors
                    for (var contributor : contributors) {
                        var membership = new NamespaceMembership();
                        membership.setNamespace(namespace);
                        membership.setUser(contributor);
                        membership.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);
                        entityManager.persist(membership);
                    }
                    count[1]++;
                }
            }
        });

        if (count[0] > 0)
            logger.info("Deleted " + count[0] + " namespaces that were orphaned and empty.");
        if (count[1] > 0)
            logger.info("Assigned explicit members to " + count[1] + " orphaned namespaces.");
        if (count[2] > 0)
            logger.info("Found " + count[2] + " orphaned namespaces that could not be fixed.");
    }
}