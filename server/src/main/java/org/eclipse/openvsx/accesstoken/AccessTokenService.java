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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.TrustedPublisher;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UUIDService;
import org.eclipse.openvsx.util.UrlUtil;

import static java.util.Objects.requireNonNull;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Service
public class AccessTokenService {
    private final AccessTokenConfig config;
    private final UUIDService uuidService;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final MailService mail;

    public AccessTokenService(
            AccessTokenConfig config,
            UUIDService uuidService,
            EntityManager entityManager,
            RepositoryService repositories,
            MailService mail
    ) {
        this.config = config;
        this.uuidService = uuidService;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.mail = mail;
    }

    /**
     * Creates a long-lived token for user. Depending on configuration, the token expiration may be set as well.
     */
    @Transactional
    public AccessTokenJson createLongLivedAccessToken(UserData user, String description) {
        requireNonNull(user);
        final LocalDateTime expiresTimestamp = config.isTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getExpiration())
                : null;
        return createAccessToken(user, description, expiresTimestamp, null, null, null, PersonalAccessTokenType.LLT);
    }

    /**
     * Creates a one-time usable token for user. Depending on configuration, the token expiration may be set as well.
     */
    @Transactional
    public AccessTokenJson createOneTimeAccessToken(UserData user, String description) {
        requireNonNull(user);
        final LocalDateTime expiresTimestamp = config.isOttTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getOttExpiration())
                : null;
        return createAccessToken(user, description, expiresTimestamp, null, null, null, PersonalAccessTokenType.OTT);
    }

    /**
     * Creates a trusted publishing token for a trusted publisher. The token is scoped to given trusted publisher
     * associated extension only. Depending on configuration, the token expiration may be set as well.
     */
    @Transactional
    public AccessTokenJson createTrustedPublishingAccessToken(TrustedPublisher trustedPublisher, String description) {
        requireNonNull(trustedPublisher);
        final LocalDateTime expiresTimestamp = config.isTptTokenExpiryEnabled()
                ? TimeUtil.getCurrentUTC().plus(config.getTptExpiration())
                : null;
        return createAccessToken(
                trustedPublisher.getCreatedBy(),
                description,
                expiresTimestamp,
                trustedPublisher,
                null,
                null,
                PersonalAccessTokenType.TPT);
    }

    private AccessTokenJson createAccessToken(
            UserData user,
            String description,
            @Nullable LocalDateTime expiresTimestamp,
            @Nullable TrustedPublisher trustedPublisher,
            @Nullable Extension scopeExtension,
            @Nullable Namespace scopeNamespace,
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
        if (trustedPublisher != null) {
            // fool-proofing; only TPT token may be created with TP
            if (type != PersonalAccessTokenType.TPT) {
                throw new IllegalArgumentException("Only TPT token my be created with TP");
            }
            // link TP and scope to TP.ext
            token.setTrustedPublisher(trustedPublisher);
            token.setScopeExtension(trustedPublisher.getExtension());
        } else if (scopeExtension != null) {
            // scope to ext
            token.setScopeExtension(scopeExtension);
        } else if (scopeNamespace != null) {
            // scope to ns
            token.setScopeNamespace(scopeNamespace);
        }

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
            value = config.getPrefix() + uuidService.generateRandomUUID().toString();
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
        // existence + active
        if (token == null || !token.isActive()) {
            return null;
        }
        // expiration
        LocalDateTime now = TimeUtil.getCurrentUTC();
        if (token.getExpiresTimestamp() != null && token.getExpiresTimestamp().isBefore(now)) {
            token.setActive(false);
            return null;
        }
        // TPT without TP => registration was deleted
        if (token.getType() == PersonalAccessTokenType.TPT && token.getTrustedPublisher() == null) {
            token.setActive(false);
            return null;
        }
        // scope
        AccessTokenScope scope = getScope(token);
        if (!scope.allowsAction(accessTokenAction)) {
            return null;
        }
        // bookkeeping; if "using"
        if (accessTokenAction.isUsing()) {
            token.setAccessedTimestamp(now);
            if (token.getType().isOneTime()) {
                token.setActive(false);
            }
        }
        return token;
    }

    private AccessTokenScope getScope(PersonalAccessToken token) {
        if (token.getScopeExtension() != null) {
            return new AccessTokenScope.ExtensionScoped(token.getScopeExtension());
        } else if (token.getScopeNamespace() != null) {
            return new AccessTokenScope.NamespaceScoped(token.getScopeNamespace());
        } else {
            return new AccessTokenScope.Unrestricted();
        }
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
        return repositories.updateExpiresTimeForLegacyPersonalAccessTokens(expirationTime, PersonalAccessTokenType.LLT);
    }
}
