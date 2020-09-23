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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "User",
    description = "User data"
)
@JsonInclude(Include.NON_NULL)
public class UserJson extends ResultJson {

    public static UserJson error(String message) {
        var user = new UserJson();
        user.error = message;
        return user;
    }

    @ApiModelProperty("Login name")
    @NotNull
    public String loginName;

    @ApiModelProperty(hidden = true)
    public String tokensUrl;

    @ApiModelProperty(hidden = true)
    public String createTokenUrl;

    @ApiModelProperty(hidden = true)
    public String role;

    @ApiModelProperty("Full name")
    public String fullName;

    @ApiModelProperty("URL to the user's avatar image")
    public String avatarUrl;

    @ApiModelProperty("URL to the user's profile page")
    public String homepage;

    @ApiModelProperty("Authentication provider (e.g. github)")
    public String provider;

}