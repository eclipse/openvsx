/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
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
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.stereotype.Component;

@Component
public class FixTargetPlatformsService {

    private final RepositoryService repositories;
    private final EntityManager entityManager;

    public FixTargetPlatformsService(RepositoryService repositories, EntityManager entityManager) {
        this.repositories = repositories;
        this.entityManager = entityManager;
    }

    @Transactional
    public UserData getUser() {
        var userName = "FixTargetPlatformMigration";
        var user = repositories.findUserByLoginName("system", userName);
        if(user == null) {
            user = new UserData();
            user.setProvider("system");
            user.setLoginName(userName);
            entityManager.persist(user);
        }
        return user;
    }
}
