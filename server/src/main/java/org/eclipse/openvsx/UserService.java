/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    protected static final String GITHUB_API = "https://api.github.com/";

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    public OAuth2User getOAuth2Principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            var principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User) {
                return (OAuth2User) principal;
            }
        }
        return null;
    }

    @Transactional
    public UserData updateUser(OAuth2User principal) {
        String url = principal.getAttribute("url");
        if (url != null && url.startsWith(GITHUB_API)) {
            return updateGitHubUser(principal);
        }
        throw new IllegalArgumentException("Unsupported principal: " + principal.getName());
    }

    protected UserData updateGitHubUser(OAuth2User principal) {
        var user = repositories.findUser("github", principal.getName());
        if (user == null) {
            user = new UserData();
            user.setProvider("github");
            user.setProviderId(principal.getName());
            user.setLoginName(principal.getAttribute("login"));
            user.setFullName(principal.getAttribute("name"));
            user.setEmail(principal.getAttribute("email"));
            user.setAvatarUrl(principal.getAttribute("avatar_url"));
            entityManager.persist(user);
            if (repositories.findNamespace(user.getLoginName()) == null) {
                createNamespace(user, user.getLoginName());
            }
        } else {
            String loginName = principal.getAttribute("login");
            if (loginName != null && !loginName.equals(user.getLoginName()))
                user.setLoginName(loginName);
            String fullName = principal.getAttribute("name");
            if (fullName != null && !fullName.equals(user.getFullName()))
                user.setFullName(fullName);
            String email = principal.getAttribute("email");
            if (email != null && !email.equals(user.getEmail()))
                user.setEmail(email);
            String avatarUrl = principal.getAttribute("avatar_url");
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl()))
                user.setAvatarUrl(avatarUrl);
        }
        return user;
    }

    @Transactional
    public Namespace createNamespace(UserData user, String name) {
        var namespace = new Namespace();
        namespace.setName(name);
        entityManager.persist(namespace);

        var membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        membership.setRole(NamespaceMembership.ROLE_OWNER);
        entityManager.persist(membership);
        return namespace;
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = repositories.findAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        return token;
    }

    public String generateTokenValue() {
        String value;
        do {
            value = UUID.randomUUID().toString();
        } while (repositories.findAccessToken(value) != null);
        return value;
    }

    public boolean hasPublishPermission(UserData user, Namespace namespace) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            return false;
        }
        return membership.getRole().equals(NamespaceMembership.ROLE_OWNER);
    }

}