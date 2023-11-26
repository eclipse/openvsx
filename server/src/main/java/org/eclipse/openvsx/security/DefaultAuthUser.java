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

import org.springframework.security.oauth2.core.user.OAuth2User;

public class DefaultAuthUser implements AuthUser {

    final String authId;
    final String avatarUrl;
    final String email;
    final String fullName;
    final String loginName;
    final String providerId;
    final String providerUrl;

    public DefaultAuthUser(String providerId, OAuth2User oauth2User) {
        authId = oauth2User.getName();
        avatarUrl = oauth2User.getAttribute("avatar_url");
        email = oauth2User.getAttribute("email");
        fullName = oauth2User.getAttribute("name");
        loginName = oauth2User.getAttribute("login");
        this.providerId = providerId;
        providerUrl = oauth2User.getAttribute("html_url");
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
