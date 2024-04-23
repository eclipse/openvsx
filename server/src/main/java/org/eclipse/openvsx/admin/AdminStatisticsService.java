/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.admin;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.AdminStatistics;
import org.springframework.stereotype.Component;

@Component
public class AdminStatisticsService {

    private final EntityManager entityManager;

    public AdminStatisticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void saveAdminStatistics(AdminStatistics statistics) {
        entityManager.persist(statistics);
    }
}
