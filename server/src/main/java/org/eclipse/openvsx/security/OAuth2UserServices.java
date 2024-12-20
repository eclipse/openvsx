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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;
import static org.eclipse.openvsx.entities.UserData.ROLE_ADMIN;
import static org.eclipse.openvsx.entities.UserData.ROLE_PRIVILEGED;
import static org.eclipse.openvsx.security.CodedAuthException.ECLIPSE_MISMATCH_GITHUB_ID;
import static org.eclipse.openvsx.security.CodedAuthException.ECLIPSE_MISSING_GITHUB_ID;
import static org.eclipse.openvsx.security.CodedAuthException.INVALID_USER;
import static org.eclipse.openvsx.security.CodedAuthException.NEED_MAIN_LOGIN;
import static org.eclipse.openvsx.security.CodedAuthException.UNSUPPORTED_REGISTRATION;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.AuthUserFactory.MissingProvider;
import org.eclipse.openvsx.util.ErrorResultException;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;

@Service
public class OAuth2UserServices {

    private static final DefaultOAuth2UserService springOAuth2UserService = new DefaultOAuth2UserService();
    private static final OidcUserService springOidcUserService = new OidcUserService();

    private final UserService users;
    private final TokenService tokens;
    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final EclipseService eclipse;
    private final AuthUserFactory authUserFactory;

    public OAuth2UserServices(
            UserService users,
            TokenService tokens,
            RepositoryService repositories,
            EntityManager entityManager,
            EclipseService eclipse,
            AuthUserFactory authUserFactory
    ) {
        this.users = users;
        this.tokens = tokens;
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.eclipse = eclipse;
        this.authUserFactory = authUserFactory;
    }

    public OAuth2UserService<OAuth2UserRequest, OAuth2User> getOauth2() { return this::loadUser; }
    public OAuth2UserService<OidcUserRequest, OidcUser> getOidc() { return this::loadUser; }

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
        return switch (userRequest.getClientRegistration().getRegistrationId()) {
            case "eclipse" -> loadEclipseUser(userRequest);
            default -> loadGenericUser(userRequest);
        };
    }

    private OAuth2User springLoadUser(OAuth2UserRequest userRequest) {
        return userRequest instanceof OidcUserRequest oidcRequest
            ? springOidcUserService.loadUser(oidcRequest)
            : springOAuth2UserService.loadUser(userRequest);
    }

    private AuthUser loadAuthUser(OAuth2UserRequest userRequest) {
        try {
            return authUserFactory.createAuthUser(userRequest.getClientRegistration().getRegistrationId(), springLoadUser(userRequest));
        } catch (MissingProvider e) {
            throw new CodedAuthException(e.getMessage(), UNSUPPORTED_REGISTRATION);
        }
    }

    private IdPrincipal loadGenericUser(OAuth2UserRequest userRequest) {
        var authUser = loadAuthUser(userRequest);
        if (StringUtils.isEmpty(authUser.getLoginName())) {
            throw new CodedAuthException("Invalid login: missing 'login' field.", INVALID_USER);
        }
        var userData = repositories.findUserByLoginName(authUser.getProviderId(), authUser.getLoginName());
        if (userData == null) {
            userData = users.registerNewUser(authUser);
        } else {
            users.updateExistingUser(userData, authUser);
        }
        return new IdPrincipal(userData.getId(), authUser.getAuthId(), getAuthorities(userData));
    }

    private IdPrincipal loadEclipseUser(OAuth2UserRequest userRequest) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            throw new CodedAuthException("Please log in with GitHub before connecting your Eclipse account.",
                    NEED_MAIN_LOGIN);
        if (!(authentication.getPrincipal() instanceof IdPrincipal))
            throw new CodedAuthException("The current authentication is invalid.", NEED_MAIN_LOGIN);
        var principal = (IdPrincipal) authentication.getPrincipal();
        var userData = entityManager.find(UserData.class, principal.getId());
        if (userData == null)
            throw new CodedAuthException("The current authentication has no backing data.", NEED_MAIN_LOGIN);
        try {
            var accessToken = userRequest.getAccessToken().getTokenValue();
            var profile = eclipse.getUserProfile(accessToken);
            if (StringUtils.isEmpty(profile.getGithubHandle()))
                throw new CodedAuthException("Your Eclipse profile is missing a GitHub username.",
                        ECLIPSE_MISSING_GITHUB_ID);
            if (!profile.getGithubHandle().equalsIgnoreCase(userData.getLoginName()))
                throw new CodedAuthException("The GitHub username setting in your Eclipse profile ("
                        + profile.getGithubHandle()
                        + ") does not match your GitHub authentication ("
                        + userData.getLoginName() + ").",
                        ECLIPSE_MISMATCH_GITHUB_ID);

            eclipse.updateUserData(userData, profile);
            return principal;
        } catch (ErrorResultException exc) {
            throw new AuthenticationServiceException(exc.getMessage(), exc);
        }
    }

    private Collection<GrantedAuthority> getAuthorities(UserData userData) {
        return switch (requireNonNullElse(userData.getRole(), "")) {
            case ROLE_ADMIN -> createAuthorityList("ROLE_ADMIN");
            case ROLE_PRIVILEGED -> createAuthorityList("ROLE_PRIVILEGED");
            default -> emptyList();
        };
    }
}
