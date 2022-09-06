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

import java.io.Serializable;
import java.util.Objects;

public class UserDataDTO  implements Serializable {

    private final long id;
    private final String role;
    private final String loginName;
    private final String fullName;
    private final String avatarUrl;
    private final String providerUrl;
    private final String provider;

    public UserDataDTO(
            long id,
            String role,
            String loginName,
            String fullName,
            String avatarUrl,
            String providerUrl,
            String provider
    ) {
        this.id = id;
        this.role = role;
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

    public String getRole() {
        return role;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDataDTO that = (UserDataDTO) o;
        return id == that.id
                && Objects.equals(role, that.role)
                && Objects.equals(loginName, that.loginName)
                && Objects.equals(fullName, that.fullName)
                && Objects.equals(avatarUrl, that.avatarUrl)
                && Objects.equals(providerUrl, that.providerUrl)
                && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, loginName, fullName, avatarUrl, providerUrl, provider);
    }
}
