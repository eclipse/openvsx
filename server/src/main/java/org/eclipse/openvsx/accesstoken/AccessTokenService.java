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

import org.springframework.stereotype.Service;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;

@Service
public class AccessTokenService {
    private final TransientAccessTokenService transientAccessTokenService;
    private final PersistentAccessTokenService persistentAccessTokenService;

    public AccessTokenService(
            TransientAccessTokenService transientAccessTokenService,
            PersistentAccessTokenService persistentAccessTokenService
    ) {
        this.transientAccessTokenService = transientAccessTokenService;
        this.persistentAccessTokenService = persistentAccessTokenService;
    }

    public AccessTokenJson createAccessToken(UserData user, String description) {
        return createAccessToken(user, description, false);
    }

    public AccessTokenJson createAccessToken(UserData user, String description, boolean oneTime) {
        if (oneTime) {
            return transientAccessTokenService.createAccessToken(user, description);
        } else {
            return persistentAccessTokenService.createAccessToken(user, description);
        }
    }

    public ResultJson deactivateAccessToken(UserData user, long id) {
        return persistentAccessTokenService.deactivateAccessToken(user, id);
    }

    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = transientAccessTokenService.useAccessToken(tokenValue);
        if (token == null) {
            token = persistentAccessTokenService.useAccessToken(tokenValue);
        }
        return token;
    }

    public int expireAccessTokens() {
        return persistentAccessTokenService.expireAccessTokens();
    }

    public void scheduleTokenExpirationNotification(PersonalAccessToken token) {
        persistentAccessTokenService.scheduleTokenExpirationNotification(token);
    }

    public void scheduleTokenExpiredMail(PersonalAccessToken token) {
        persistentAccessTokenService.scheduleTokenExpiredMail(token);
    }

    public int setExpirationTimeForLegacyAccessTokens(LocalDateTime expirationTime) {
        return persistentAccessTokenService.setExpirationTimeForLegacyAccessTokens(expirationTime);
    }
}
