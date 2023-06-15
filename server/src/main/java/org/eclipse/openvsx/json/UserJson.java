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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(
    name = "User",
    description = "User data"
)
@JsonInclude(Include.NON_NULL)
public class UserJson extends ResultJson implements Serializable {

    public static UserJson error(String message) {
        var user = new UserJson();
        user.error = message;
        return user;
    }

    @Schema(description = "Login name")
    @NotNull
    public String loginName;

    @Schema(hidden = true)
    public String tokensUrl;

    @Schema(hidden = true)
    public String createTokenUrl;

    @Schema(hidden = true)
    public String role;

    @Schema(description = "Full name")
    public String fullName;

    @Schema(description = "URL to the user's avatar image")
    public String avatarUrl;

    @Schema(description = "URL to the user's profile page")
    public String homepage;

    @Schema(description = "Authentication provider (e.g. github)")
    public String provider;

    @Schema(hidden = true)
    public PublisherAgreement publisherAgreement;

    @Schema(hidden = true)
    public List<UserJson> additionalLogins;

    @JsonInclude(Include.NON_NULL)
    public static class PublisherAgreement implements Serializable {

        /* 'none' | 'signed' | 'outdated' */
        public String status;

        public String timestamp;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PublisherAgreement that = (PublisherAgreement) o;
            return Objects.equals(status, that.status) && Objects.equals(timestamp, that.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, timestamp);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserJson userJson = (UserJson) o;
        return Objects.equals(loginName, userJson.loginName)
                && Objects.equals(tokensUrl, userJson.tokensUrl)
                && Objects.equals(createTokenUrl, userJson.createTokenUrl)
                && Objects.equals(role, userJson.role)
                && Objects.equals(fullName, userJson.fullName)
                && Objects.equals(avatarUrl, userJson.avatarUrl)
                && Objects.equals(homepage, userJson.homepage)
                && Objects.equals(provider, userJson.provider)
                && Objects.equals(publisherAgreement, userJson.publisherAgreement)
                && Objects.equals(additionalLogins, userJson.additionalLogins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loginName, tokensUrl, createTokenUrl, role, fullName, avatarUrl, homepage, provider, publisherAgreement, additionalLogins);
    }
}