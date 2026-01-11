/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.eclipse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.UserData;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TokenService {

    protected final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final TransactionTemplate transactions;
    private final EntityManager entityManager;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public TokenService(
            TransactionTemplate transactions,
            EntityManager entityManager,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.transactions = transactions;
        this.entityManager = entityManager;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public AuthToken updateEclipseToken(long userId, OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        var token = toAuthToken(accessToken, refreshToken);
        return transactions.execute(status -> {
            var userData = entityManager.find(UserData.class, userId);
            userData.setEclipseToken(token);
            return token;
        });
    }

    private AuthToken toAuthToken(OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        if(accessToken == null) {
            return null;
        }

        String refresh = null;
        Instant refreshExpiresAt = null;
        if (refreshToken != null) {
            refresh = refreshToken.getTokenValue();
            refreshExpiresAt = refreshToken.getExpiresAt();
        }

        return new AuthToken(
                accessToken.getTokenValue(),
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt(),
                accessToken.getScopes(),
                refresh,
                refreshExpiresAt
        );
    }

    public AuthToken getActiveEclipseToken(UserData userData) {
        var token = userData.getEclipseToken();
        if (token != null && isExpired(token.expiresAt())) {
            OAuth2AccessToken newAccessToken = null;
            OAuth2RefreshToken newRefreshToken = null;
            var newTokens = refreshEclipseToken(token);
            if (newTokens != null) {
                newAccessToken = newTokens.getFirst();
                newRefreshToken = newTokens.getSecond();
            }

            return updateEclipseToken(userData.getId(), newAccessToken, newRefreshToken);
        }
        return token;
    }

    private boolean isExpired(Instant instant) {
        return instant != null && Instant.now().isAfter(instant);
    }

    private Pair<OAuth2AccessToken, OAuth2RefreshToken> refreshEclipseToken(AuthToken token) {
        if(token.refreshToken() == null || isExpired(token.refreshExpiresAt())) {
            return null;
        }

        var reg = Optional.ofNullable(clientRegistrationRepository).map(repo -> repo.findByRegistrationId("eclipse")).orElse(null);
        if(reg == null) {
            logger.error("Eclipse client not registered");
            return null;
        }

        var tokenUri = reg.getProviderDetails().getTokenUri();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        var objectMapper = new ObjectMapper();

        var data = new LinkedMultiValueMap<>();
        data.add("grant_type", "refresh_token");
        data.add("client_id", reg.getClientId());
        data.add("client_secret", reg.getClientSecret());
        data.add("refresh_token", token.refreshToken());

        try {
            var request = new HttpEntity<>(data, headers);
            var restTemplate = new RestTemplate();
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
        } catch (HttpClientErrorException.BadRequest exc) {
            // keycloak sends a 400 status response if the refresh call failed
            logger.warn("Eclipse token could not be refreshed: {}", exc.getMessage());
        } catch (RestClientException exc) {
            logger.error("Post request failed with URL: {}", tokenUri, exc);
        } catch (JsonProcessingException exc) {
            logger.error("Invalid JSON data received from URL: {}", tokenUri, exc);
        }
        return null;
    }

}