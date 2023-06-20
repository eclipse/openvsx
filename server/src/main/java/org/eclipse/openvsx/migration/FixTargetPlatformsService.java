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

import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Component
public class FixTargetPlatformsService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Transactional
    public UserData getUser() {
        var userName = "FixTargetPlatformMigration";
        var user = repositories.findUserByLoginName(null, userName);
        if(user == null) {
            user = new UserData();
            user.setLoginName(userName);
            entityManager.persist(user);
        }
        return user;
    }
}
