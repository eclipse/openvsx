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

import java.util.Objects;

public class PersonalAccessTokenDTO {
    private final UserDataDTO user;

    public PersonalAccessTokenDTO(
            Long userId,
            String loginName,
            String fullName,
            String avatarUrl,
            String providerUrl,
            String provider
    ) {
        this.user = new UserDataDTO(userId, loginName, fullName, avatarUrl, providerUrl, provider);
    }

    public UserDataDTO getUser() {
        return user;
    }
}
