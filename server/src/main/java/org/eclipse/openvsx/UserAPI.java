/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.util.Arrays;

import com.google.common.base.Strings;

import org.eclipse.openvsx.json.UserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAPI {

    @Autowired
    UserService users;

    @Value("${ovsx.webui.url}")
    String webuiUrl;

    @GetMapping(
        value = "/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserJson> userData(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                                             @AuthenticationPrincipal OAuth2User principal) {
        var user = users.updateUser(authorizedClient, principal);
        var json = user.toUserJson();
        return new ResponseEntity<>(json, getUserHeaders(), HttpStatus.OK);
    }

    private HttpHeaders getUserHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!Strings.isNullOrEmpty(webuiUrl)) {
            headers.setAccessControlAllowOrigin(webuiUrl);
            headers.setAccessControlAllowCredentials(true);
            headers.setAccessControlAllowHeaders(Arrays.asList("Content-Type"));
        }
        return headers;
    }

}