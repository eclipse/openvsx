/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.CsrfTokenJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.NamespaceMembershipJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserAPI {

    private final static int TOKEN_DESCRIPTION_SIZE = 255;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UserService users;

    @GetMapping(
        path = "/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserJson getUserData() {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            return UserJson.error("Not logged in.");
        }
        var user = users.updateUser(principal);
        var json = user.toUserJson();
        var serverUrl = UrlUtil.getBaseUrl();
        json.tokensUrl = createApiUrl(serverUrl, "user", "tokens");
        json.createTokenUrl = createApiUrl(serverUrl, "user", "token", "create");
        return json;
    }

    @GetMapping(
        path = "/user/csrf",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CsrfTokenJson getCsrfToken(HttpServletRequest request) {
        var csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken == null) {
            return CsrfTokenJson.error("Token is not available.");
        }
        var json = new CsrfTokenJson();
        json.value = csrfToken.getToken();
        json.header = csrfToken.getHeaderName();
        return json;
    }

    @GetMapping(
        path = "/user/tokens",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<AccessTokenJson> getAccessTokens() {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var serverUrl = UrlUtil.getBaseUrl();
        return repositories.findAccessTokens(user)
                .filter(token -> token.isActive())
                .map(token -> {
                    var json = token.toAccessTokenJson();
                    json.deleteTokenUrl = createApiUrl(serverUrl, "user", "token", "delete", Long.toString(token.getId()));
                    return json;
                })
                .toList();
    }

    @PostMapping(
        path = "/user/token/create",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(rollbackOn = ResponseStatusException.class)
    public AccessTokenJson createAccessToken(@RequestParam(required = false) String description) {
        if (description != null && description.length() > TOKEN_DESCRIPTION_SIZE) {
            return AccessTokenJson.error("The description must not be longer than " + TOKEN_DESCRIPTION_SIZE + " characters.");
        }
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(users.generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        token.setDescription(description);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.value = token.getValue();
        var serverUrl = UrlUtil.getBaseUrl();
        json.deleteTokenUrl = createApiUrl(serverUrl, "user", "token", "delete", Long.toString(token.getId()));
        return json;
    }

    @PostMapping(
        path = "/user/token/delete/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson deleteAccessToken(@PathVariable long id) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var token = repositories.findAccessToken(id);
        if (token == null || !token.isActive() || !token.getUser().equals(user)) {
            return ResultJson.error("Token does not exist.");
        }
        token.setActive(false);
        return ResultJson.success("Deleted access token for user " + user.getLoginName());
    }

    @GetMapping(
        path = "/user/namespaces",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<NamespaceJson> getOwnNamespaces() {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);

        var memberships = repositories.findMemberships(user, NamespaceMembership.ROLE_OWNER);

        return memberships.map(membership -> {
            var namespace = membership.getNamespace();
            var json = new NamespaceJson();
            json.name = namespace.getName();
            json.extensions = new LinkedHashMap<>();
            var serverUrl = UrlUtil.getBaseUrl();
            for (var ext : repositories.findExtensions(namespace)) {
                String url = createApiUrl(serverUrl, "api", namespace.getName(), ext.getName());
                json.extensions.put(ext.getName(), url);
            }
            json.access = NamespaceJson.RESTRICTED_ACCESS;
            json.addMembershipUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "member", "add");
            json.getMembersUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "members");

            return json;
        }).toList();
    }

    @GetMapping(
        path = "/user/namespace/{name}/members",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<NamespaceMembershipJson> getNamespaceMembers(@PathVariable String name) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var namespace = repositories.findNamespace(name);
        var userMembership = repositories.findMembership(user, namespace);
        if(userMembership != null && userMembership.getRole().equals(NamespaceMembership.ROLE_OWNER)) {
            var memberships = repositories.findMemberships(namespace);
            return memberships.map(membership -> membership.toJson()).toList();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN); 
        }
    }

    @GetMapping(
        path = "/user/search/{name}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<UserJson> getUsersStartWith(@PathVariable String name) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var users = repositories.findUsersByLoginNameStartingWith(name);

        return users.filter(user -> user.getProvider() != null).map(user -> user.toUserJson()).toList();
    }

    @PostMapping(path = "/user/namespace/{namespaceName}/member/{setMethod}/{userLogin}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson setNamespaceMember(@PathVariable String namespaceName, @PathVariable String userLogin,
            @PathVariable String setMethod, @RequestParam(required = false) String provider) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var namespace = repositories.findNamespace(namespaceName);
        var userMembership = repositories.findMembership(user, namespace);
        var member = repositories.findUserByLoginName(provider, userLogin);

        if (userMembership != null && userMembership.getRole().equals(NamespaceMembership.ROLE_OWNER)) {
            if (setMethod.equalsIgnoreCase("add")) {
                users.addNamespaceMember(namespace, member, NamespaceMembership.ROLE_CONTRIBUTOR, user);
                return ResultJson.success("Added " + userLogin + " as member to namespace " + namespace);
            } else if (setMethod.equalsIgnoreCase("remove")) {
                users.removeNamespaceMember(namespace, member, user);
                return ResultJson.success("Removed " + userLogin + " from namespace " + namespace);
            } else {
                return ResultJson.error("Set method is not correct. Must be either 'add' or 'remove'.");
            }
        }
        
        return ResultJson.error("You must be an owner of this namespace.");
    }

    @PostMapping(path = "/user/namespace/{namespaceName}/role/{userLogin}/{role}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson setNamespaceMemberRole(@PathVariable String namespaceName, @PathVariable String userLogin,
            @PathVariable String role, @RequestParam(required = false) String provider) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        users.editNamespaceMember(namespaceName, userLogin, provider, role, user);

        return ResultJson.success("Changed role for " + userLogin + " in namespace " + namespaceName + " to " + role);
    }
}