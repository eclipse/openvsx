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

import java.util.Optional;

import org.eclipse.openvsx.json.UserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAPI {

    @Autowired
    UserService users;

    @GetMapping(
        value = "/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UserJson userData() {
        var authentication = users.getAuthentication();
        if (authentication != null) {
            var principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User) {
                var user = users.updateUser((OAuth2User) principal, Optional.empty());
                return user.toUserJson();
            }
        }
        return UserJson.error("Not logged in.");
    }

}