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
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.CsrfTokenJson;
import org.eclipse.openvsx.json.DeleteTokenResultJson;
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
    public AccessTokenJson createAccessToken(@RequestParam(name = "description", required = false) String description) {
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
    public DeleteTokenResultJson deleteAccessToken(@PathVariable("id") long id) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var user = users.updateUser(principal);
        var token = repositories.findAccessToken(id);
        if (token == null || !token.isActive() || !token.getUser().equals(user)) {
            return DeleteTokenResultJson.error("Token does not exist.");
        }
        token.setActive(false);
        var json = new DeleteTokenResultJson();
        return json;
    }

}