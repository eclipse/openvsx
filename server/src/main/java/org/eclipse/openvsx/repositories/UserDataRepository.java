/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

public interface UserDataRepository extends Repository<UserData, Long> {

    UserData findByProviderAndLoginName(String provider, String loginName);

    Page<UserData> findByLoginNameStartingWith(String loginNameStart, Pageable page);

    long count();

    Streamable<UserData> findByProvider(String provider);
}