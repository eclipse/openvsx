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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.util.TimeUtil;

@Service
public class TransientAccessTokenService {
    private final AccessTokenConfig config;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ConcurrentHashMap<String, PersonalAccessToken> transientTokens;

    public TransientAccessTokenService(
            AccessTokenConfig config
    ) {
        this.config = config;
        this.transientTokens = new ConcurrentHashMap<>();
    }

    public AccessTokenJson createAccessToken(UserData user, String description) {
        // monotonic grow + random - to make sure it is unique, and it does not clash with persistent ones
        String value = config.getPrefix() + "ot" + UUID.randomUUID() + "-" + counter.incrementAndGet();
        var token = new PersonalAccessToken();
        token.setId(0L); // ID is not used for transient tokens
        token.setUser(user);
        token.setValue(value);
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setNotified(false);
        token.setDescription(description);
        transientTokens.put(value, token);
        return token.toAccessTokenJson();
    }

    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = transientTokens.remove(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(TimeUtil.getCurrentUTC());
        return token;
    }
}
