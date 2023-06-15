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

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.CodedAuthException;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.eclipse.openvsx.util.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class UserAPI {

    private final static int TOKEN_DESCRIPTION_SIZE = 255;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UserService users;

    @Autowired
    EclipseService eclipse;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    VersionService versions;

    /**
     * Redirect to GitHub Oauth2 login as default login provider.
     */
    @GetMapping(
        path = "/login"
    )
    public ModelAndView login(ModelMap model) {
        return new ModelAndView("redirect:/oauth2/authorization/github", model);
    }

    /**
     * Retrieve the last authentication error and return its details.
     */
    @GetMapping(
        path = "/user/auth-error",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ErrorJson getAuthError(HttpServletRequest request) {
        var authException = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (!(authException instanceof AuthenticationException))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var json = new ErrorJson();
        json.message = ((AuthenticationException) authException).getMessage();
        if (authException instanceof CodedAuthException)
            json.code = ((CodedAuthException) authException).getCode();
        return json;
    }

    /**
     * This endpoint is used to check whether there is a logged-in user. For this reason, it
     * does not return a 403 status, but an OK status with JSON body when no user data is
     * available. This is to avoid unnecessary network error logging in the browser console.
     */
    @GetMapping(
        path = "/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserJson getUserData() {
        var user = users.findLoggedInUser();
        if (user == null) {
            return UserJson.error("Not logged in.");
        }
        var json = user.toUserJson();
        var serverUrl = UrlUtil.getBaseUrl();
        json.role = user.getRole();
        json.tokensUrl = createApiUrl(serverUrl, "user", "tokens");
        json.createTokenUrl = createApiUrl(serverUrl, "user", "token", "create");
        eclipse.enrichUserJson(json, user);
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
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
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
    public ResponseEntity<AccessTokenJson> createAccessToken(@RequestParam(required = false) String description) {
        if (description != null && description.length() > TOKEN_DESCRIPTION_SIZE) {
            var json = AccessTokenJson.error("The description must not be longer than " + TOKEN_DESCRIPTION_SIZE + " characters.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        var user = users.findLoggedInUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(users.createAccessToken(user, description), HttpStatus.CREATED);
    }

    @PostMapping(
        path = "/user/token/delete/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteAccessToken(@PathVariable long id) {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            return ResponseEntity.ok(users.deleteAccessToken(user, id));
        } catch(NotFoundException e) {
            return new ResponseEntity<>(ResultJson.error("Token does not exist."), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
            path = "/user/extensions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<ExtensionJson> getOwnExtensions() {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var types = new String[]{ DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST };
        return repositories.findExtensions(user)
                .map(e -> versions.getLatestTrxn(e, null, false, false))
                .map(latest -> {
                    var json = latest.toExtensionJson();
                    json.preview = latest.isPreview();
                    json.active = latest.getExtension().isActive();
                    json.files = storageUtil.getFileUrls(latest, UrlUtil.getBaseUrl(), types);

                    return json;
                })
                .toList();
    }

    @GetMapping(
        path = "/user/namespaces",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<NamespaceJson> getOwnNamespaces() {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var memberships = repositories.findMemberships(user, NamespaceMembership.ROLE_OWNER)
                .and(repositories.findMemberships(user, NamespaceMembership.ROLE_CONTRIBUTOR));

        return memberships.map(membership -> {
            var namespace = membership.getNamespace();
            var json = new NamespaceJson();
            json.name = namespace.getName();
            json.extensions = new LinkedHashMap<>();
            var serverUrl = UrlUtil.getBaseUrl();
            for (var ext : repositories.findActiveExtensions(namespace)) {
                String url = createApiUrl(serverUrl, "api", namespace.getName(), ext.getName());
                json.extensions.put(ext.getName(), url);
            }

            var isOwner = membership.getRole().equals(NamespaceMembership.ROLE_OWNER);
            json.verified = isOwner || repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0;
            if(isOwner) {
                json.membersUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "members");
                json.roleUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "role");
            }

            return json;
        }).toList();
    }

    @PostMapping(
        path = "/user/namespace/{namespace}/details",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> updateNamespaceDetails(@RequestBody NamespaceDetailsJson details) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                    .body(users.updateNamespaceDetails(details));
        } catch (NotFoundException exc) {
            var json = NamespaceDetailsJson.error("Namespace not found: " + details.name);
            return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(ResultJson.class);
        }
    }

    @GetMapping(
        path = "/user/namespace/{name}/members",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NamespaceMembershipListJson> getNamespaceMembers(@PathVariable String name) {
        var user = users.findLoggedInUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        var namespace = repositories.findNamespace(name);
        var userMembership = repositories.findMembership(user, namespace);
        if (userMembership != null && userMembership.getRole().equals(NamespaceMembership.ROLE_OWNER)) {
            var memberships = repositories.findMemberships(namespace);
            var membershipList = new NamespaceMembershipListJson();
            membershipList.namespaceMemberships = memberships.map(membership -> membership.toJson()).toList();
            return new ResponseEntity<>(membershipList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(NamespaceMembershipListJson.error("You don't have the permission to see this."), HttpStatus.FORBIDDEN); 
        }
    }

    @PostMapping(
        path = "/user/namespace/{namespace}/role",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> setNamespaceMember(@PathVariable String namespace, @RequestParam String user,
            @RequestParam String role, @RequestParam(required = false) String provider) {
        var requestingUser = users.findLoggedInUser();
        if (requestingUser == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            var json = users.setNamespaceMember(requestingUser, namespace, provider, user, role);
            return ResponseEntity.ok(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(
        path = "/user/search/{name}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<UserJson> getUsersStartWith(@PathVariable String name) {
        if (users.findLoggedInUser() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var users = repositories.findUsersByLoginNameStartingWith(name)
                .map(user -> user.toUserJson());
        return CollectionUtil.limit(users, 5);
    }

    @PostMapping(
        path = "/user/publisher-agreement",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserJson> signPublisherAgreement() {
        var user = users.findLoggedInUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            eclipse.signPublisherAgreement(user);
            return ResponseEntity.ok(getUserData());
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(UserJson.class);
        }
    }

}