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

import org.eclipse.openvsx.json.UserJson;

import java.util.Objects;

public class UserDataDTO {

    private final long id;
    private final String loginName;
    private final String fullName;
    private final String avatarUrl;
    private final String providerUrl;
    private final String provider;

    public UserDataDTO(
            long id,
            String loginName,
            String fullName,
            String avatarUrl,
            String providerUrl,
            String provider
    ) {
        this.id = id;
        this.loginName = loginName;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.providerUrl = providerUrl;
        this.provider = provider;
    }

    /**
     * Convert to a JSON object.
     */
    public UserJson toUserJson() {
        var json = new UserJson();
        json.loginName = this.getLoginName();
        json.fullName = this.getFullName();
        json.avatarUrl = this.getAvatarUrl();
        json.homepage = this.getProviderUrl();
        json.provider = this.getProvider();
        return json;
    }

    public long getId() { return id; }

    public String getLoginName() {
        return loginName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public String getProvider() {
        return provider;
    }
}
