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

import org.springframework.stereotype.Service;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;

@Service
public class AccessTokenService {
    private final PersonalAccessTokens personalAccessTokens;

    public AccessTokenService(
            PersonalAccessTokens personalAccessTokens
    ) {
        this.personalAccessTokens = personalAccessTokens;
    }

    /**
     * Shorthand for {@link #createAccessToken(UserData, String, boolean)} with {@code oneTime=false}.
     */
    public AccessTokenJson createAccessToken(UserData user, String description) {
        return createAccessToken(user, description, false);
    }

    /**
     * Creates access token.
     */
    public AccessTokenJson createAccessToken(UserData user, String description, boolean oneTime) {
        return personalAccessTokens.createAccessToken(user, description, oneTime);
    }

    /**
     * Looks up and prepare for use the token.
     */
    public PersonalAccessToken useAccessToken(String tokenValue) {
        return personalAccessTokens.useAccessToken(tokenValue);
    }

    public ResultJson deactivateAccessToken(UserData user, long id) {
        return personalAccessTokens.deactivateAccessToken(user, id);
    }
}
