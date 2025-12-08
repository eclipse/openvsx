/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class UserPublishInfoJson extends ResultJson {

    public static UserPublishInfoJson error(String message) {
        var userPublishInfo = new UserPublishInfoJson();
        userPublishInfo.setError(message);
        return userPublishInfo;
    }

    @NotNull
    private UserJson user;

    @NotNull
    private List<ExtensionJson> extensions;

    @NotNull
    private List<ReviewJson> reviews;

    @NotNull
    private Integer activeAccessTokenNum;

    public UserJson getUser() {
        return user;
    }

    public void setUser(UserJson user) {
        this.user = user;
    }

    public List<ExtensionJson> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<ExtensionJson> extensions) {
        this.extensions = extensions;
    }

    public List<ReviewJson> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewJson> reviews) {
        this.reviews = reviews;
    }

    public Integer getActiveAccessTokenNum() {
        return activeAccessTokenNum;
    }

    public void setActiveAccessTokenNum(Integer activeAccessTokenNum) {
        this.activeAccessTokenNum = activeAccessTokenNum;
    }
}