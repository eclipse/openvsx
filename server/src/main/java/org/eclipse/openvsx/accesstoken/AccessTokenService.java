/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.accesstoken;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Service
public class AccessTokenService {
    private final AccessTokenConfig config;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final MailService mail;

    public AccessTokenService(
            AccessTokenConfig config,
            EntityManager entityManager,
            RepositoryService repositories,
            MailService mail
    ) {
        this.config = config;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.mail = mail;
    }

    @Transactional
    public AccessTokenJson createLongLivedAccessToken(UserData user, String description) {
        final LocalDateTime expiresTimestamp = config.isTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getExpiration())
                : null;
        return createAccessToken(user, description, expiresTimestamp, PersonalAccessTokenType.LLT);
    }

    @Transactional
    public AccessTokenJson createOneTimeAccessToken(UserData user, String description) {
        final LocalDateTime expiresTimestamp = config.isOttTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getOttExpiration())
                : null;
        return createAccessToken(user, description, expiresTimestamp, PersonalAccessTokenType.OTT);
    }

    @Transactional
    public AccessTokenJson createTrustedPublishingAccessToken(UserData user, String description) {
        final LocalDateTime expiresTimestamp = config.isTptTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getTptExpiration())
                : null;
        return createAccessToken(user, description, expiresTimestamp, PersonalAccessTokenType.TPT);
    }

    private AccessTokenJson createAccessToken(
            UserData user,
            String description,
            @Nullable LocalDateTime expiresTimestamp,
            PersonalAccessTokenType type
    ) {
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setDescription(description);
        token.setExpiresTimestamp(expiresTimestamp);
        token.setType(type);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.setValue(token.getValue());
        if (!type.isOneTime()) {
            json.setDeleteTokenUrl(
                    createApiUrl(UrlUtil.getBaseUrl(), "user", "token", "delete", Long.toString(token.getId())));
        }

        return json;
    }

    // public to be accessible from tests
    public String generateTokenValue() {
        String value;
        do {
            value = config.getPrefix() + UUID.randomUUID();
        } while (repositories.hasPersonalAccessToken(value));
        return value;
    }

    @Transactional
    public ResultJson deactivateAccessToken(UserData user, long id) {
        var token = repositories.findPersonalAccessToken(id);
        if (token == null || !token.isActive()) {
            throw new NotFoundException();
        }

        user = entityManager.merge(user);
        if (!token.getUser().equals(user)) {
            throw new NotFoundException();
        }

        token.setActive(false);
        return ResultJson.success("Deactivated access token for user " + user.getLoginName() + ".");
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue, AccessTokenAction accessTokenAction) {
        var token = repositories.findPersonalAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        LocalDateTime now = TimeUtil.getCurrentUTC();
        if (token.getExpiresTimestamp() != null && token.getExpiresTimestamp().isBefore(now)) {
            token.setActive(false);
            return null;
        }
        token.setAccessedTimestamp(now);
        if (accessTokenAction.isUsing() && token.getType().isOneTime()) {
            token.setActive(false);
        }
        return token;
    }

    @Transactional
    public int expireAccessTokens() {
        var expiredAccessTokens = repositories.expirePersonalAccessTokens(TimeUtil.getCurrentUTC());
        if (config.isSendExpiredMailEnabled()) {
            for (var token : expiredAccessTokens) {
                if (token.getType().isNotify()) {
                    mail.scheduleAccessTokenExpiredMail(token);
                }
            }
        }
        return expiredAccessTokens.size();
    }

    @Transactional
    public void scheduleTokenExpirationNotification(PersonalAccessToken token) {
        token = entityManager.merge(token);
        if (token.getType().isNotify() && !token.isNotified()) {
            try {
                mail.scheduleAccessTokenExpiryNotification(token);
            } finally {
                token.setNotified(true);
            }
        }
    }

    @Transactional
    public int setExpirationTimeForLegacyAccessTokens(LocalDateTime expirationTime) {
        return repositories.updateExpiresTimeForLegacyPersonalAccessTokens(expirationTime);
    }
}
