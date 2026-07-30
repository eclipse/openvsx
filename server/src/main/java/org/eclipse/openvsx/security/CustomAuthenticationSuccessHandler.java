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

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final List<OAuth2LoginHandler> loginHandlers;

    public CustomAuthenticationSuccessHandler(String defaultTargetUrl, List<OAuth2LoginHandler> loginHandlers) {
        setDefaultTargetUrl(defaultTargetUrl);
        this.loginHandlers = loginHandlers;
    }

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            var token = (OAuth2AuthenticationToken) authentication;
            var registrationId = token.getAuthorizedClientRegistrationId();
            var targetUrl = loginHandlers.stream()
                    .filter(handler -> handler.getRegistrationId().equals(registrationId))
                    .map(handler -> handler.getSuccessRedirectUrl(getDefaultTargetUrl()))
                    .filter(url -> url != null)
                    .findFirst();
            if (targetUrl.isPresent()) {
                return targetUrl.get();
            }
        }
        return determineTargetUrl(request, response);
    }

}
