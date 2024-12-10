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

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.CodedAuthException;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@RestController
public class UserAPI {

    private final static int TOKEN_DESCRIPTION_SIZE = 255;

    protected final Logger logger = LoggerFactory.getLogger(UserAPI.class);

    private final RepositoryService repositories;
    private final UserService users;
    private final EclipseService eclipse;
    private final StorageUtilService storageUtil;
    private final OVSXConfig config;

    public UserAPI(
            RepositoryService repositories,
            UserService users,
            EclipseService eclipse,
            StorageUtilService storageUtil,
            OVSXConfig config
    ) {
        this.repositories = repositories;
        this.users = users;
        this.eclipse = eclipse;
        this.storageUtil = storageUtil;
        this.config = config;
    }

    /**
     * Redirect to GitHub Oauth2 login as default login provider.
     */
    @GetMapping(
        path = "/login"
    )
    public ModelAndView login(ModelMap model) {
        return new ModelAndView("redirect:/oauth2/authorization/" + config.getAuth().getProvider(), model);
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

        var code = authException instanceof CodedAuthException ? ((CodedAuthException) authException).getCode() : null;
        return new ErrorJson(((AuthenticationException) authException).getMessage(), code);
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
        json.setRole(user.getRole());
        json.setTokensUrl(createApiUrl(serverUrl, "user", "tokens"));
        json.setCreateTokenUrl(createApiUrl(serverUrl, "user", "token", "create"));
        eclipse.enrichUserJson(json, user);
        return json;
    }

    @GetMapping(
        path = "/user/csrf",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CsrfTokenJson getCsrfToken(HttpServletRequest request) {
        var csrfToken = (CsrfToken) request.getAttribute("_csrf");
        return csrfToken != null
                ? new CsrfTokenJson(csrfToken.getToken(), csrfToken.getHeaderName())
                : CsrfTokenJson.error("Token is not available.");
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
        return repositories.findActiveAccessTokens(user)
                .map(token -> {
                    var json = token.toAccessTokenJson();
                    json.setDeleteTokenUrl(createApiUrl(serverUrl, "user", "token", "delete", Long.toString(token.getId())));
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

        var extVersions = repositories.findLatestVersions(user);
        var types = new String[]{ DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST };
        var fileUrls = storageUtil.getFileUrls(extVersions, UrlUtil.getBaseUrl(), types);
        return extVersions.stream()
                .map(latest -> {
                    var json = latest.toExtensionJson();
                    json.setPreview(latest.isPreview());
                    json.setActive(latest.getExtension().isActive());
                    json.setFiles(fileUrls.get(latest.getId()));
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

        return repositories.findMemberships(user).map(membership -> {
            var namespace = membership.getNamespace();
            var extensions = new LinkedHashMap<String, String>();
            var serverUrl = UrlUtil.getBaseUrl();
            repositories.findActiveExtensionsForUrls(namespace).forEach(extension -> {
                String url = createApiUrl(serverUrl, "api", namespace.getName(), extension.getName());
                extensions.put(extension.getName(), url);
            });

            var json = new NamespaceJson();
            json.setName(namespace.getName());
            json.setExtensions(extensions);
            var isOwner = membership.getRole().equals(NamespaceMembership.ROLE_OWNER);
            json.setVerified(isOwner || repositories.hasMemberships(namespace, NamespaceMembership.ROLE_OWNER));
            if(isOwner) {
                json.setMembersUrl(createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "members"));
                json.setRoleUrl(createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "role"));
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
            var json = NamespaceDetailsJson.error("Namespace not found: " + details.getName());
            return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(ResultJson.class);
        }
    }

    @PostMapping(
            path = "/user/namespace/{namespace}/details/logo",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResultJson> updateNamespaceDetailsLogo(
            @PathVariable String namespace,
            @RequestParam MultipartFile file
    ) {
        try {
            return ResponseEntity.ok()
                    .body(users.updateNamespaceDetailsLogo(namespace, file));
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

        var memberships = repositories.findMembershipsForOwner(user, name);
        if (!memberships.isEmpty()) {
            var membershipList = new NamespaceMembershipListJson();
            membershipList.setNamespaceMemberships(memberships.stream().map(NamespaceMembership::toJson).toList());
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

        return repositories.findUsersByLoginNameStartingWith(name, 5).stream()
                .map(UserData::toUserJson)
                .toList();
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