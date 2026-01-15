/** ******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.Tier;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface TierRepository extends Repository<Tier, Long> {

    List<Tier> findAll();

    Tier findByNameIgnoreCase(String name);

    long count();
}
