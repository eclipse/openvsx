/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;

public interface PersonalAccessTokenRepository extends Repository<PersonalAccessToken, Long> {

    Streamable<PersonalAccessToken> findAll();

    Streamable<PersonalAccessToken> findByUser(UserData user);

    Streamable<PersonalAccessToken> findByUserAndActiveTrue(UserData user);

    long countByUserAndActiveTrue(UserData user);

    PersonalAccessToken findById(long id);

    PersonalAccessToken findByValue(String value);

    PersonalAccessToken findByUserAndDescriptionAndActiveTrue(UserData user, String description);

    @Modifying
    @Query("update PersonalAccessToken t set t.active = false where t.user = ?1 and t.active = true")
    int updateActiveSetFalse(UserData user);

    Streamable<PersonalAccessToken> findByCreatedTimestampLessThanEqualAndActiveTrue(LocalDateTime timestamp);

    @Modifying
    @Query("update PersonalAccessToken t set t.active = false where t.createdTimestamp <= ?1 and t.active = true")
    void expireAccessTokens(LocalDateTime timestamp);
}