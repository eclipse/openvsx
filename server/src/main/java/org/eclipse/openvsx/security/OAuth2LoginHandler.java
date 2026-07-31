/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

/**
 * Deployment-specific handling of logins for a single OAuth2 client registration,
 * e.g. a provider that links a secondary account to the logged-in user instead of
 * creating one. Registrations without a handler go through the generic
 * attribute-mapping flow.
 */
public interface OAuth2LoginHandler {

    /**
     * The client registration id this handler applies to.
     */
    String getRegistrationId();

    /**
     * Load the principal for a login with this provider.
     */
    IdPrincipal loadUser(OAuth2UserRequest userRequest);

    /**
     * Called after a successful login with this provider.
     */
    default void authenticationSucceeded(
            IdPrincipal principal,
            OAuth2AccessToken accessToken,
            OAuth2RefreshToken refreshToken
    ) {
    }

    /**
     * Target URL to redirect to after a successful login, or {@code null} for the default.
     */
    default String getSuccessRedirectUrl(String defaultTargetUrl) {
        return null;
    }
}
