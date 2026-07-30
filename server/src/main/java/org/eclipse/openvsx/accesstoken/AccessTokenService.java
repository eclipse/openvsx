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

    /**
     * Shorthand for {@link #createAccessToken(UserData, String, boolean)} with {@code oneTime=false}.
     */
    @Transactional
    public AccessTokenJson createAccessToken(UserData user, String description) {
        return createAccessToken(user, description, false);
    }

    @Transactional
    public AccessTokenJson createAccessToken(UserData user, String description, boolean oneTime) {
        final LocalDateTime expiresTimestamp;
        if (oneTime) {
            expiresTimestamp = config.isOttTokenExpiryEnabled()
                    ? TimeUtil.getCurrentUTC().plus(config.getOttExpiration())
                    : null;
        } else {
            expiresTimestamp = config.isTokenExpiryEnabled()
                    ? TimeUtil.getCurrentUTC().plus(config.getExpiration())
                    : null;
        }
        return createAccessToken(
                user,
                description,
                expiresTimestamp,
                oneTime);
    }

    private AccessTokenJson createAccessToken(
            UserData user,
            String description,
            @Nullable LocalDateTime expiresTimestamp,
            boolean oneTime
    ) {
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setDescription(description);
        token.setExpiresTimestamp(expiresTimestamp);
        token.setType(oneTime ? PersonalAccessTokenType.OTT : PersonalAccessTokenType.LLT);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.setValue(token.getValue());
        if (!oneTime) {
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
    public UserData verifyAccessToken(String tokenValue) {
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
        return token.getUser();
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue) {
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
        if (token.getType().isOneTime()) {
            // it is OTT; pull it out immediately on first use
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
