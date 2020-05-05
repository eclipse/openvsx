/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;;

@JsonInclude(Include.NON_NULL)
public class UserJson extends ResultJson {

    public static UserJson error(String message) {
        var user = new UserJson();
        user.error = message;
        return user;
    }

    public String loginName;

    public String tokensUrl;

    public String createTokenUrl;

    @Nullable
    public String fullName;

    @Nullable
    public String avatarUrl;

    @Nullable
    public String homepage;

    @Nullable
    public String provider;

}