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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Service
public class AccessTokenService {
    private final EntityManager entityManager;
    private final RepositoryService repositories;

    @Value("${ovsx.token-prefix:}")
    String tokenPrefix;

    public AccessTokenService(EntityManager entityManager, RepositoryService repositories) {
        this.entityManager = entityManager;
        this.repositories = repositories;
    }

    @Transactional
    public AccessTokenJson createAccessToken(UserData user, String description) {
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setDescription(description);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.setValue(token.getValue());
        json.setDeleteTokenUrl(createApiUrl(UrlUtil.getBaseUrl(), "user", "token", "delete", Long.toString(token.getId())));

        return json;
    }

    // public to be accessible from tests
    public String generateTokenValue() {
        String value;
        do {
            value = tokenPrefix + UUID.randomUUID();
        } while (repositories.hasAccessToken(value));
        return value;
    }

    @Transactional
    public ResultJson deleteAccessToken(UserData user, long id) {
        var token = repositories.findAccessToken(id);
        if (token == null || !token.isActive()) {
            throw new NotFoundException();
        }

        user = entityManager.merge(user);
        if(!token.getUser().equals(user)) {
            throw new NotFoundException();
        }

        token.setActive(false);
        return ResultJson.success("Deleted access token for user " + user.getLoginName() + ".");
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = repositories.findAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(TimeUtil.getCurrentUTC());
        return token;
    }
}
