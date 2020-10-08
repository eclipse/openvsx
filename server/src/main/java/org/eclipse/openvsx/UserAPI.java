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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.EclipseData;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.EclipseData.PublisherAgreement;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.CsrfTokenJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.NamespaceMembershipListJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.json.simple.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserAPI {

    private final static int TOKEN_DESCRIPTION_SIZE = 255;

    public final static String PUBLISHER_AGREEMENT_URI = "https://api.eclipse.org/openvsx/publisher_agreement";

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UserService users;

    @Value("${ovsx.eclipse.publisher-agreement.version:}")
    String publisherAgreementVersion;

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

        // Add information on the Eclipse Publisher Agreement
        if (!Strings.isNullOrEmpty(publisherAgreementVersion)) {
            var eclipseData = user.getEclipseData();
            if (eclipseData == null || eclipseData.publisherAgreement == null) {
                json.publisherAgreement = "none";
            } else {
                if (eclipseData.publisherAgreement.signedVersion == null)
                    json.publisherAgreement = "none";
                else if (publisherAgreementVersion.equals(eclipseData.publisherAgreement.signedVersion))
                    json.publisherAgreement = "signed";
                else
                    json.publisherAgreement = "outdated";
                if (eclipseData.publisherAgreement.signedTimestamp != null)
                    json.publisherAgreementTimestamp = TimeUtil.toUTCString(eclipseData.publisherAgreement.signedTimestamp);
                if (eclipseData.personId != null) {
                    var eclipseLogin = new UserJson();
                    eclipseLogin.provider = "eclipse";
                    eclipseLogin.loginName = eclipseData.personId;
                    json.additionalLogins = Lists.newArrayList(eclipseLogin);
                }
            }
        }
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
    @Transactional
    public ResponseEntity<AccessTokenJson> createAccessToken(@RequestParam(required = false) String description) {
        if (description != null && description.length() > TOKEN_DESCRIPTION_SIZE) {
            var json = AccessTokenJson.error("The description must not be longer than " + TOKEN_DESCRIPTION_SIZE + " characters.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        var user = users.findLoggedInUser();
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(users.generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setDescription(description);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.value = token.getValue();
        var serverUrl = UrlUtil.getBaseUrl();
        json.deleteTokenUrl = createApiUrl(serverUrl, "user", "token", "delete", Long.toString(token.getId()));
        return new ResponseEntity<>(json, HttpStatus.CREATED);
    }

    @PostMapping(
        path = "/user/token/delete/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<ResultJson> deleteAccessToken(@PathVariable long id) {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var token = repositories.findAccessToken(id);
        if (token == null || !token.isActive() || !token.getUser().equals(user)) {
            var json = ResultJson.error("Token does not exist.");
            return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
        }
        token.setActive(false);
        var json = ResultJson.success("Deleted access token for user " + user.getLoginName() + ".");
        return ResponseEntity.ok(json);
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
            json.membersUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "members");
            json.roleUrl = createApiUrl(serverUrl, "user", "namespace", namespace.getName(), "role");
            return json;
        }).toList();
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
            var json = ResultJson.error(exc.getMessage());
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
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
    public ResultJson signPublisherAgreement() {
        var currentUser = users.findLoggedInUser();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        AuthToken eclipseToken = currentUser.getEclipseToken();
        if (eclipseToken == null) {
            return ResultJson.error("Authorization required");
        }

        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(eclipseToken.accessToken);

        var data = new JsonObject();
        data.put("version", 1);

        var objectMapper = new ObjectMapper();

        var request = new HttpEntity<String>(data.toJson(), headers);
        try {
            var response = restTemplate.postForObject(PUBLISHER_AGREEMENT_URI, request, String.class);
            var root = objectMapper.readTree(response);

            var ed = new EclipseData();
            var pub = ed.new PublisherAgreement();
            ed.publisherAgreement = pub;
            ed.personId = root.get("PersonID").asText();
            pub.documentId = root.get("DocumentID").asText();
            pub.signedVersion = root.get("Version").asText();
            pub.signedTimestamp = LocalDateTime.parse(root.get("EffectiveDate").asText(), CUSTOM_DATE_TIME);
            pub.isActive = "1".equals(root.get("SysDocument").get("IsActive").asText());

            users.updateUser(currentUser.getId(), user -> {
                user.setEclipseData(ed);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResultJson.success("Signed the publisher agreement");
    }

    public static final DateTimeFormatter CUSTOM_DATE_TIME;
    static {
        CUSTOM_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .toFormatter();
    }

}