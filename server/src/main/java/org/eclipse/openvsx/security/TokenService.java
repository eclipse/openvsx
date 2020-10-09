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

import java.time.Instant;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.UserData;
import org.json.simple.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TokenService {

    @Autowired
    UserService users;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @Transactional
    public void updateTokens(long userId, String registrationId, OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        UserData userData = entityManager.find(UserData.class, userId);
        if (userData == null) {
            return;
        }

        if ("eclipse".equals(registrationId)) {
            AuthToken token = null;
            if (accessToken != null) {
                token = new AuthToken();
                token.accessToken = accessToken.getTokenValue();
                token.scopes = accessToken.getScopes();
                token.issuedAt = accessToken.getIssuedAt();
                token.expiresAt = accessToken.getExpiresAt();
                
                if (refreshToken != null) {
                    token.refreshToken = refreshToken.getTokenValue();
                } else {
                    var tokens = refreshEclipseToken(registrationId, token);
                    if (tokens != null) {
                        token.accessToken = tokens.getFirst().getTokenValue();
                        token.scopes = tokens.getFirst().getScopes();
                        token.issuedAt = tokens.getFirst().getIssuedAt();
                        token.expiresAt = tokens.getFirst().getExpiresAt();
                        token.refreshToken = tokens.getSecond().getTokenValue();
                    }
                }
            }

            userData.setEclipseToken(token);
        }
        if ("github".equals(registrationId)) {
            AuthToken token = null;
            if (accessToken != null) {
                token = new AuthToken();
                token.accessToken = accessToken.getTokenValue();
                token.scopes = accessToken.getScopes();
                token.issuedAt = accessToken.getIssuedAt();
                token.expiresAt = accessToken.getExpiresAt();
            }

            userData.setGithubToken(token);
        }

        entityManager.persist(userData);
    }

    @Transactional
    public String getAccessToken(long userId, String registrationId) {
        UserData userData = entityManager.find(UserData.class, userId);
        if (userData == null) {
            return null;
        }
        if ("github".equals(registrationId)) {
            if (userData.getGithubToken() != null) {
                return userData.getGithubToken().accessToken;
            }
        }
        if ("eclipse".equals(registrationId)) {
            var token = userData.getEclipseToken();
            if (token != null) {
                if (Instant.now().isAfter(token.expiresAt)) {
                    var tokens = refreshEclipseToken(registrationId, token);

                    if (tokens != null && tokens.getFirst() != null) {
                        updateTokens(userId, registrationId, tokens.getFirst(), tokens.getSecond());
                        return tokens.getFirst().getTokenValue();
                    }
                    if (tokens == null) {
                        updateTokens(userId, registrationId, null, null);
                    }
                } else {
                    return token.accessToken;
                }
            }
        }

        return null;
    }

    protected Pair<OAuth2AccessToken, OAuth2RefreshToken> refreshEclipseToken(String registrationId, AuthToken token) {
        var reg = clientRegistrationRepository.findByRegistrationId(registrationId);
        var tokenUri = reg.getProviderDetails().getTokenUri();

        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var data = new JsonObject();
        data.put("grant_type", "refresh_token");
        data.put("client_id", reg.getClientId());
        data.put("client_secret", reg.getClientSecret());
        data.put("refresh_token", token.accessToken);

        var objectMapper = new ObjectMapper();

        var request = new HttpEntity<String>(data.toJson(), headers);
        try {
            var response = restTemplate.postForObject(tokenUri, request, String.class);
            var root = objectMapper.readTree(response);
            var newTokenValue = root.get("access_token").asText();
            var newRefreshTokenValue = root.get("refresh_token").asText();
            var expires_in = root.get("expires_in").asLong();

            var issuedAt = Instant.now();
            var expiresAt = issuedAt.plusSeconds(expires_in);

            var newToken = new OAuth2AccessToken(TokenType.BEARER, newTokenValue, issuedAt, expiresAt);
            var newRefreshToken = new OAuth2RefreshToken(newRefreshTokenValue, issuedAt);
            return Pair.of(newToken, newRefreshToken);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}