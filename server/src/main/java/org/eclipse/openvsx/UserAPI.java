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
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.DeleteTokenResultJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAPI {

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

    @Autowired
    RepositoryService repositories;

    @GetMapping(
        path = "/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserJson getUserData() {
        var authentication = users.getAuthentication();
        if (authentication != null) {
            var principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User) {
                var user = users.updateUser((OAuth2User) principal, Optional.empty());
                var json = user.toUserJson();
                var serverUrl = UrlUtil.getBaseUrl();
                json.tokensUrl = createApiUrl(serverUrl, "user", "tokens");
                json.createTokenUrl = createApiUrl(serverUrl, "user", "token", "create");
                json.deleteTokenUrl = createApiUrl(serverUrl, "user", "token", "delete");
                return json;
            }
        }
        return UserJson.error("Not logged in.");
    }

    @GetMapping(
        path = "/user/tokens",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<AccessTokenJson> getTokens(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                                           @AuthenticationPrincipal OAuth2User principal) {
        var user = users.updateUser(principal, Optional.of(authorizedClient));
        return repositories.findAccessTokens(user)
                .map(t -> t.toAccessTokenJson())
                .toList();
    }

    @PostMapping(
        path = "/user/token/create",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public AccessTokenJson createToken(@RequestParam(name = "description", required = false) String description,
                                       @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                                       @AuthenticationPrincipal OAuth2User principal) {
        var user = users.updateUser(principal, Optional.of(authorizedClient));
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(users.generateTokenValue());
        token.setCreatedTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        token.setAccessedTimestamp(token.getCreatedTimestamp());
        token.setDescription(description);
        entityManager.persist(token);
        return token.toAccessTokenJson();
    }

    @DeleteMapping(
        path = "/user/token/delete",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public DeleteTokenResultJson deleteToken(@RequestParam(name = "token") String tokenValue,
                                             @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                                             @AuthenticationPrincipal OAuth2User principal) {
        var user = users.updateUser(principal, Optional.of(authorizedClient));
        var token = repositories.findAccessToken(user, tokenValue);
        if (token == null) {
            return DeleteTokenResultJson.error("Token does not exist.");
        }
        entityManager.remove(token);
        var json = new DeleteTokenResultJson();
        return json;
    }

}