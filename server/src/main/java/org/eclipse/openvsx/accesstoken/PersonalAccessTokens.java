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
import org.springframework.stereotype.Component;

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

/**
 * Service handling long-living "Personal Access Tokens".
 */
@Component
public class PersonalAccessTokens {
    private final AccessTokenConfig config;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final MailService mail;

    public PersonalAccessTokens(
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
    public AccessTokenJson createAccessToken(UserData user, String description, boolean oneTime) {
        return createAccessToken(
                user,
                description,
                config.isTokenExpiryEnabled()
                        ? TimeUtil.getCurrentUTC().plus(config.getExpiration())
                        : null,
                oneTime);
    }

    protected AccessTokenJson createAccessToken(
            UserData user,
            String description,
            LocalDateTime expiresTimestamp,
            boolean oneTime
    ) {
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setType(oneTime ? PersonalAccessTokenType.OTT : PersonalAccessTokenType.PAT);
        token.setExpiresTimestamp(expiresTimestamp);
        token.setDescription(description);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.setValue(token.getValue());
        json.setDeleteTokenUrl(
                createApiUrl(UrlUtil.getBaseUrl(), "user", "token", "delete", Long.toString(token.getId())));

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
    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = repositories.findPersonalAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(TimeUtil.getCurrentUTC());
        return token;
    }

    public int expireAccessTokens() {
        var expiredAccessTokens = repositories.expirePersonalAccessTokens(TimeUtil.getCurrentUTC());
        if (config.isSendExpiredMailEnabled()) {
            for (var token : expiredAccessTokens) {
                mail.scheduleAccessTokenExpiredMail(token);
            }
        }
        return expiredAccessTokens.size();
    }

    @Transactional
    public void scheduleTokenExpirationNotification(PersonalAccessToken token) {
        token = entityManager.merge(token);
        try {
            mail.scheduleAccessTokenExpiryNotification(token);
        } finally {
            token.setNotified(true);
        }
    }

    public void scheduleTokenExpiredMail(PersonalAccessToken token) {
        mail.scheduleAccessTokenExpiredMail(token);
    }

    @Transactional
    public int setExpirationTimeForLegacyAccessTokens(LocalDateTime expirationTime) {
        return repositories.updateExpiresTimeForLegacyPersonalAccessTokens(expirationTime);
    }
}
