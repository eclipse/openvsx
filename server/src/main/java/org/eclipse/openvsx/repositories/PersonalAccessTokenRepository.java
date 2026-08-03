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

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;

public interface PersonalAccessTokenRepository extends Repository<PersonalAccessToken, Long> {

    Streamable<PersonalAccessToken> findAll();

    Streamable<PersonalAccessToken> findByUser(UserData user);

    Streamable<PersonalAccessToken> findByUserAndActiveTrueAndType(UserData user, PersonalAccessTokenType type);

    long countByUserAndActiveTrueAndType(UserData user, PersonalAccessTokenType type);

    PersonalAccessToken findById(long id);

    PersonalAccessToken findByValue(String value);

    PersonalAccessToken findByUserAndDescriptionAndActiveTrue(UserData user, String description);

    @Modifying
    @Query("update PersonalAccessToken t set t.active = false where t.user = ?1 and t.active = true")
    int updateActiveSetFalse(UserData user);

    @Modifying
    @Query(
        "update PersonalAccessToken t set t.expiresTimestamp = ?1 where t.active = true and t.expiresTimestamp is null and t.type = ?2"
    )
    int updateExpiresTimeForLegacyAccessTokens(LocalDateTime timestamp, PersonalAccessTokenType type);

    List<PersonalAccessToken> findByExpiresTimestampLessThanEqualAndActiveTrueAndNotifiedFalseOrderById(
            LocalDateTime timestamp,
            Pageable pageable
    );

    @Modifying
    @Query(
        value = "update personal_access_token set active = false where expires_timestamp <= ?1 and active = true returning *",
        nativeQuery = true
    )
    List<PersonalAccessToken> expireAccessTokens(LocalDateTime timestamp);
}
