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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.Namespace;
import org.springframework.stereotype.Component;

@Component
public class NamespaceLogoFileResourceService {

    private final EntityManager entityManager;

    public NamespaceLogoFileResourceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Namespace getNamespace(long entityId) {
        return entityManager.find(Namespace.class, entityId);
    }

    @Transactional
    public void updateNamespace(Namespace namespace) {
        entityManager.merge(namespace);
    }
}
