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
import java.util.Arrays;

import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.UserData;
import org.json.simple.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class TokenService {

    protected final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    public AuthToken updateTokens(long userId, String registrationId, OAuth2AccessToken accessToken,
            OAuth2RefreshToken refreshToken) {
        var userData = entityManager.find(UserData.class, userId);
        if (userData == null) {
            return null;
        }
    
        switch (registrationId) {
            case "github": {
                if (accessToken == null) {
                    return updateGitHubToken(userData, null);
                }
                var token = new AuthToken();
                token.accessToken = accessToken.getTokenValue();
                token.scopes = accessToken.getScopes();
                token.issuedAt = accessToken.getIssuedAt();
                token.expiresAt = accessToken.getExpiresAt();
                return updateGitHubToken(userData, token);
            }

            case "eclipse": {
                if (accessToken == null) {
                    return updateEclipseToken(userData, null);
                }
                var token = new AuthToken();
                token.accessToken = accessToken.getTokenValue();
                token.scopes = accessToken.getScopes();
                token.issuedAt = accessToken.getIssuedAt();
                token.expiresAt = accessToken.getExpiresAt();
                
                if (refreshToken != null) {
                    token.refreshToken = refreshToken.getTokenValue();
                    token.refreshExpiresAt = refreshToken.getExpiresAt();
                } else {
                    // Request a new token to get the refresh token
                    var tokens = refreshEclipseToken(token);
                    if (tokens != null) {
                        token.accessToken = tokens.getFirst().getTokenValue();
                        token.scopes = tokens.getFirst().getScopes();
                        token.issuedAt = tokens.getFirst().getIssuedAt();
                        token.expiresAt = tokens.getFirst().getExpiresAt();
                        token.refreshToken = tokens.getSecond().getTokenValue();
                        token.refreshExpiresAt = tokens.getSecond().getExpiresAt();
                    }
                }
                return updateEclipseToken(userData, token);
            }
        }
        return null;
    }

    private AuthToken updateGitHubToken(UserData userData, AuthToken token) {
        return transactions.execute(status -> {
            userData.setGithubToken(token);
            entityManager.merge(userData);
            return token;
        });
    }

    private AuthToken updateEclipseToken(UserData userData, AuthToken token) {
        return transactions.execute(status -> {
            userData.setEclipseToken(token);
            entityManager.merge(userData);
            return token;
        });
    }

    public AuthToken getActiveToken(UserData userData, String registrationId) {
        switch (registrationId) {
            case "github": {
                return userData.getGithubToken();
            }

            case "eclipse": {
                var token = userData.getEclipseToken();
                if (token != null && isExpired(token.expiresAt)) {
                    OAuth2AccessToken newAccessToken = null;
                    OAuth2RefreshToken newRefreshToken = null;
                    if (!isExpired(token.refreshExpiresAt)) {
                        var newTokens = refreshEclipseToken(token);
                        if (newTokens != null) {
                            newAccessToken = newTokens.getFirst();
                            newRefreshToken = newTokens.getSecond();
                        }
                    }
                    return updateTokens(userData.getId(), "eclipse", newAccessToken, newRefreshToken);
                }
                return token;
            }
        }

        return null;
    }

    public boolean isUsable(AuthToken token) {
        if (token == null)
            return false;
        if (token.accessToken != null && !isExpired(token.expiresAt))
            return true;
        if (token.refreshToken != null && !isExpired(token.refreshExpiresAt))
            return true;
        return false;
    }

    private boolean isExpired(Instant instant) {
        return instant != null && Instant.now().isAfter(instant);
    }

    protected Pair<OAuth2AccessToken, OAuth2RefreshToken> refreshEclipseToken(AuthToken token) {
        var reg = clientRegistrationRepository.findByRegistrationId("eclipse");
        var tokenUri = reg.getProviderDetails().getTokenUri();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        var data = new JsonObject();
        data.put("grant_type", "refresh_token");
        data.put("client_id", reg.getClientId());
        data.put("client_secret", reg.getClientSecret());
        data.put("refresh_token", token.refreshToken != null ? token.refreshToken : token.accessToken);

        var request = new HttpEntity<String>(data.toJson(), headers);
        var restTemplate = new RestTemplate();
        var objectMapper = new ObjectMapper();
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

        } catch (RestClientException exc) {
            logger.error("Post request failed with URL: " + tokenUri, exc);
        } catch (JsonProcessingException exc) {
            logger.error("Invalid JSON data received from URL: " + tokenUri, exc);
        }
        return null;
    }

}