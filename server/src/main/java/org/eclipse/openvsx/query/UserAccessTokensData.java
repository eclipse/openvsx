/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.query;

import org.eclipse.openvsx.json.AccessTokenJson;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

@RedisHash("UserAccessTokens")
public class UserAccessTokensData {
    @Id
    private long userId;
    private List<AccessTokenJson> accessTokens;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public List<AccessTokenJson> getAccessTokens() {
        return accessTokens;
    }

    public void setAccessTokens(List<AccessTokenJson> accessTokens) {
        this.accessTokens = accessTokens;
    }
}
