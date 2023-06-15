/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public CustomAuthenticationSuccessHandler(String defaultTargetUrl) {
        setDefaultTargetUrl(defaultTargetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            var token = (OAuth2AuthenticationToken) authentication;
            // Redirect to user profile page after login to Eclipse
            if ("eclipse".equals(token.getAuthorizedClientRegistrationId())) {
                return UrlUtil.createApiUrl(getDefaultTargetUrl(), "user-settings", "profile");
            }
        }
        return determineTargetUrl(request, response);
    }
    
}