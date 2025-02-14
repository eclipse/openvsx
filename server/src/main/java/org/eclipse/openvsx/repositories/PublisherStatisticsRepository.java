/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.PublisherStatistics;
import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

public interface PublisherStatisticsRepository extends Repository<PublisherStatistics, Long> {

    PublisherStatistics findByYearAndMonthAndUserId(int year, int month, long userId);

    Streamable<PublisherStatistics> findByUser(UserData user);
}
