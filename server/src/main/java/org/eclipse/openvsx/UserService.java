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

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
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
        var user = repositories.findUserByProviderId("github", principal.getName());
        if (user == null) {
            user = new UserData();
            user.setProvider("github");
            user.setProviderId(principal.getName());
            user.setLoginName(principal.getAttribute("login"));
            user.setFullName(principal.getAttribute("name"));
            user.setEmail(principal.getAttribute("email"));
            user.setProviderUrl(principal.getAttribute("html_url"));
            user.setAvatarUrl(principal.getAttribute("avatar_url"));
            entityManager.persist(user);
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
            String providerUrl = principal.getAttribute("html_url");
            if (providerUrl != null && !providerUrl.equals(user.getProviderUrl()))
                user.setProviderUrl(providerUrl);
            String avatarUrl = principal.getAttribute("avatar_url");
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl()))
                user.setAvatarUrl(avatarUrl);
        }
        return user;
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = repositories.findAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(TimeUtil.getCurrentUTC());
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
        if (UserData.ROLE_PRIVILEGED.equals(user.getRole())) {
            // Privileged users can publish to every namespace.
            return true;
        }

        var ownerships = repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER);
        if (ownerships.isEmpty()) {
            // If the namespace has no owner, everyone has publish permission to it.
            return true;
        }
        if (ownerships.stream().anyMatch(m -> m.getUser().equals(user))) {
            // The requesting user is an owner of the namespace.
            return true;
        }

        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            // The namespace is owned by someone else and the requesting user is not a member.
            return false;
        }
        return membership.getRole().equalsIgnoreCase(NamespaceMembership.ROLE_CONTRIBUTOR);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson setNamespaceMember(UserData requestingUser, String namespaceName, String provider, String userLogin, String role) {
        var namespace = repositories.findNamespace(namespaceName);
        var userMembership = repositories.findMembership(requestingUser, namespace);
        if ((userMembership == null || !userMembership.getRole().equals(NamespaceMembership.ROLE_OWNER)) && !requestingUser.getRole().equals("admin")) {
            throw new ErrorResultException("You must be an owner of this namespace.");
        }
        var targetUser = repositories.findUserByLoginName(provider, userLogin);
        if (targetUser == null) {
            throw new ErrorResultException("User not found: " + provider + "/" + userLogin);
        }

        if (role.equals("remove")) {
            return removeNamespaceMember(namespace, targetUser);
        } else {
            return addNamespaceMember(namespace, targetUser, role);
        }
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson removeNamespaceMember(Namespace namespace, UserData user) throws ErrorResultException {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            throw new ErrorResultException("User " + user.getLoginName() + " is not a member of " + namespace.getName() + ".");
        }
        entityManager.remove(membership);
        return ResultJson.success("Removed " + user.getLoginName() + " from namespace " + namespace.getName() + ".");
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson addNamespaceMember(Namespace namespace, UserData user, String role) {
        if (!(role.equals(NamespaceMembership.ROLE_OWNER)
                || role.equals(NamespaceMembership.ROLE_CONTRIBUTOR))) {
            throw new ErrorResultException("Invalid role: " + role);
        }
        var membership = repositories.findMembership(user, namespace);
        if (membership != null) {
            if (role.equals(membership.getRole())) {
                throw new ErrorResultException("User " + user.getLoginName() + " already has the role " + role + ".");
            }
            membership.setRole(role);
            return ResultJson.success("Changed role of " + user.getLoginName() + " in " + namespace.getName() + " to " + role + ".");
        }
        membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        membership.setRole(role);
        entityManager.persist(membership);
        return ResultJson.success("Added " + user.getLoginName() + " as " + role + " of " + namespace.getName() + ".");
    }

}