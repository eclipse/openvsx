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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.UserData;

import static java.util.Collections.emptyList;
import static org.eclipse.openvsx.security.CodedAuthException.*;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

@Service
public class OAuth2UserServices {

    private final UserService users;
    private final OAuth2AttributesConfig attributesConfig;
    private final Map<String, OAuth2LoginHandler> loginHandlers;
    private final DefaultOAuth2UserService springOAuth2UserService;
    private final OidcUserService springOidcUserService;

    public OAuth2UserServices(
            UserService users,
            OAuth2AttributesConfig attributesConfig,
            List<OAuth2LoginHandler> loginHandlers
    ) {
        this.users = users;
        this.attributesConfig = attributesConfig;
        this.loginHandlers = loginHandlers.stream()
                .collect(Collectors.toMap(OAuth2LoginHandler::getRegistrationId, Function.identity()));
        springOAuth2UserService = new DefaultOAuth2UserService();
        springOidcUserService = new OidcUserService();
    }

    public OAuth2UserService<OAuth2UserRequest, OAuth2User> getOauth2() {
        return this::loadUser;
    }
    public OAuth2UserService<OidcUserRequest, OidcUser> getOidc() {
        return this::loadUser;
    }

    @EventListener
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        // We can assume that `UserData` already exists, because this event is fired after
        // `ExtendedOAuth2UserServices.loadUser` was processed.
        if (event.getSource() instanceof OAuth2LoginAuthenticationToken) {
            var auth = (OAuth2LoginAuthenticationToken) event.getSource();
            var handler = loginHandlers.get(auth.getClientRegistration().getRegistrationId());
            if (handler != null) {
                var idPrincipal = (IdPrincipal) auth.getPrincipal();
                handler.authenticationSucceeded(idPrincipal, auth.getAccessToken(), auth.getRefreshToken());
            }
        }
    }

    public IdPrincipal loadUser(OAuth2UserRequest userRequest) {
        var handler = loginHandlers.get(userRequest.getClientRegistration().getRegistrationId());
        return handler != null ? handler.loadUser(userRequest) : loadGenericUser(userRequest);
    }

    public boolean canLogin() {
        return users.canLogin();
    }

    private IdPrincipal loadGenericUser(OAuth2UserRequest userRequest) {
        var registrationId = userRequest.getClientRegistration().getRegistrationId();
        var mapping = attributesConfig.getAttributeMapping(registrationId);
        if (mapping == null) {
            throw new CodedAuthException("Unsupported registration: " + registrationId, UNSUPPORTED_REGISTRATION);
        }

        var oauth2User = userRequest instanceof OidcUserRequest oidcRequest
                ? springOidcUserService.loadUser(oidcRequest)
                : springOAuth2UserService.loadUser(userRequest);

        var userAttributes = mapping.toUserData(registrationId, oauth2User);
        if (StringUtils.isEmpty(userAttributes.getLoginName())) {
            throw new CodedAuthException("Invalid login: missing 'login' field.", INVALID_USER);
        }

        var userData = users.upsertUser(userAttributes);
        return new IdPrincipal(userData.getId(), userData.getAuthId(), getAuthorities(userData));
    }

    private Collection<GrantedAuthority> getAuthorities(UserData userData) {
        var role = userData.getRole();
        if (role == null) {
            return emptyList();
        }
        return switch (role) {
            case ADMIN -> createAuthorityList("ROLE_ADMIN");
            case PRIVILEGED -> createAuthorityList("ROLE_PRIVILEGED");
        };
    }
}
