/********************************************************************************
 * Copyright (c) 2023 Ericsson and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

public class DefaultAuthUser implements AuthUser {

    protected final String authId;
    protected final String avatarUrl;
    protected final String email;
    protected final String fullName;
    protected final String loginName;
    protected final String providerId;
    protected final String providerUrl;

    public DefaultAuthUser(
        final String authId,
        final String avatarUrl,
        final String email,
        final String fullName,
        final String loginName,
        final String providerId,
        final String providerUrl
    ) {
        this.authId = authId;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.fullName = fullName;
        this.loginName = loginName;
        this.providerId = providerId;
        this.providerUrl = providerUrl;
    }

    @Override
    public String getAuthId() {
        return authId;
    }

    @Override
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getLoginName() {
        return loginName;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getProviderUrl() {
        return providerUrl;
    }
}
