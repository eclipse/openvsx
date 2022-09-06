/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.dto;

import java.io.Serializable;
import java.util.Objects;

public class PersonalAccessTokenDTO implements Serializable {
    private final UserDataDTO user;

    public PersonalAccessTokenDTO(
            Long userId,
            String userRole,
            String loginName,
            String fullName,
            String avatarUrl,
            String providerUrl,
            String provider
    ) {
        this.user = new UserDataDTO(userId, userRole, loginName, fullName, avatarUrl, providerUrl, provider);
    }

    public UserDataDTO getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalAccessTokenDTO that = (PersonalAccessTokenDTO) o;
        return Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }
}
