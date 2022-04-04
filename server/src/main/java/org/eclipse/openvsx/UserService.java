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

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.IdPrincipal;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    CacheService cache;

    public UserData findLoggedInUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof IdPrincipal) {
                var principal = (IdPrincipal) authentication.getPrincipal();
                return entityManager.find(UserData.class, principal.getId());
            }
        }
        return null;
    }

    @Transactional
    public UserData registerNewUser(OAuth2User oauth2User) {
        var user = new UserData();
        user.setProvider("github");
        user.setAuthId(oauth2User.getName());
        user.setLoginName(oauth2User.getAttribute("login"));
        user.setFullName(oauth2User.getAttribute("name"));
        user.setEmail(oauth2User.getAttribute("email"));
        user.setProviderUrl(oauth2User.getAttribute("html_url"));
        user.setAvatarUrl(oauth2User.getAttribute("avatar_url"));
        entityManager.persist(user);
        return user;
    }

    @Transactional
    public UserData updateExistingUser(UserData user, OAuth2User oauth2User) {
        switch (user.getProvider()) {
            case "github": {
                var updated = false;
                String loginName = oauth2User.getAttribute("login");
                if (loginName != null && !loginName.equals(user.getLoginName())) {
                    user.setLoginName(loginName);
                    updated = true;
                }
                String fullName = oauth2User.getAttribute("name");
                if (fullName != null && !fullName.equals(user.getFullName())) {
                    user.setFullName(fullName);
                    updated = true;
                }
                String email = oauth2User.getAttribute("email");
                if (email != null && !email.equals(user.getEmail())) {
                    user.setEmail(email);
                    updated = true;
                }
                String providerUrl = oauth2User.getAttribute("html_url");
                if (providerUrl != null && !providerUrl.equals(user.getProviderUrl())) {
                    user.setProviderUrl(providerUrl);
                    updated = true;
                }
                String avatarUrl = oauth2User.getAttribute("avatar_url");
                if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                    user.setAvatarUrl(avatarUrl);
                    updated = true;
                }
                if (updated) {
                    cache.evictExtensionJsons(user);
                }
                break;
            }
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

        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            // The requesting user is not a member of the namespace.
            return false;
        }
        var role = membership.getRole();
        return NamespaceMembership.ROLE_CONTRIBUTOR.equalsIgnoreCase(role)
                || NamespaceMembership.ROLE_OWNER.equalsIgnoreCase(role);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson setNamespaceMember(UserData requestingUser, String namespaceName, String provider, String userLogin, String role) {
        var namespace = repositories.findNamespace(namespaceName);
        var userMembership = repositories.findMembership(requestingUser, namespace);
        if (userMembership == null || !userMembership.getRole().equals(NamespaceMembership.ROLE_OWNER)) {
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