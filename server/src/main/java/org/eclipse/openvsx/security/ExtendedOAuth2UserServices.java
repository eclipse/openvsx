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
import java.util.Collections;

import com.google.common.base.Strings;

import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class ExtendedOAuth2UserServices {

    @Autowired
    UserService users;

    @Autowired
    TokenService tokens;
    
    @Autowired
    RepositoryService repositories;

    @Autowired
    EclipseService eclipse;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidc;

    public ExtendedOAuth2UserServices() {
        this.oauth2 = new OAuth2UserService<OAuth2UserRequest, OAuth2User>() {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                return ExtendedOAuth2UserServices.this.loadUser(userRequest);
            }
        };
        this.oidc = new OAuth2UserService<OidcUserRequest, OidcUser>() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
                return ExtendedOAuth2UserServices.this.loadUser(userRequest);
            }
        };
    }

    public OAuth2UserService<OAuth2UserRequest, OAuth2User> getOauth2() {
        return oauth2;
    }

    public OAuth2UserService<OidcUserRequest, OidcUser> getOidc() {
        return oidc;
    }

    @EventListener
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        // We can assume that `UserData` already exists, because this event is fired after
        // `ExtendedOAuth2UserServices.loadUser` was processed.
        if (event.getSource() instanceof OAuth2LoginAuthenticationToken) {
            var auth = (OAuth2LoginAuthenticationToken) event.getSource();
            var idPrincipal = (IdPrincipal) auth.getPrincipal();
            var accessToken = auth.getAccessToken();
            var refreshToken = auth.getRefreshToken();
            var registrationId = auth.getClientRegistration().getRegistrationId();

            tokens.updateTokens(idPrincipal.getId(), registrationId, accessToken, refreshToken);
        }
    }

    public IdPrincipal loadUser(OAuth2UserRequest userRequest) {
        var registrationId = userRequest.getClientRegistration().getRegistrationId();

        switch (registrationId) {
            case "github": {
                var authUser = delegate.loadUser(userRequest);
                String loginName = authUser.getAttribute("login");
                if (Strings.isNullOrEmpty(loginName))
                    throw new DisabledException("Invalid login: missing 'login' field.");
                var userData = repositories.findUserByLoginName("github", loginName);
                if (userData == null)
                    userData = users.registerNewUser(authUser);
                else
                    users.updateExistingUser(userData, authUser);
                return new IdPrincipal(userData.getId(), authUser.getName(), getAuthorities(userData));
            }

            case "eclipse": {
                try {
                    var accessToken = userRequest.getAccessToken().getTokenValue();
                    var profile = eclipse.getUserProfile(accessToken);
                    if (Strings.isNullOrEmpty(profile.githubHandle))
                        throw new DisabledException("Please set the \"GitHub Username\" in your Eclipse profile.");
                    var userData = repositories.findUserByLoginName("github", profile.githubHandle);
                    if (userData == null)
                        throw new DisabledException("The \"GitHub Username\" setting in your Eclipse profile must match the GitHub account you used to log in to this website.");
                    eclipse.updateUserData(userData, profile);
                    eclipse.getPublisherAgreement(userData, accessToken);
                    return new IdPrincipal(userData.getId(), profile.name, getAuthorities(userData));
                } catch (ErrorResultException exc) {
                    throw new DisabledException(exc.getMessage(), exc);
                }
            }

            default:
                throw new DisabledException("Invalid registration: " + registrationId);
        }
    }

    private Collection<GrantedAuthority> getAuthorities(UserData userData) {
        var role = userData.getRole();
        switch (role != null ? role : "") {
            case UserData.ROLE_ADMIN:
                return AuthorityUtils.createAuthorityList("ROLE_ADMIN");
            case UserData.ROLE_PRIVILEGED:
                return AuthorityUtils.createAuthorityList("ROLE_PRIVILEGED");
            default:
                return Collections.emptyList();
        }
    }

}