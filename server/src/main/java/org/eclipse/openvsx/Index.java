/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import org.eclipse.openvsx.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Index {

    @Autowired
    TokenService tokens;

    @Autowired
    UserService users;

    @GetMapping(
        path = "/",
        produces = MediaType.TEXT_HTML_VALUE
    )
    public String getStats() {
        var r = new StringBuilder();
        var NL = "<br/><br/>";

        var user = users.findLoggedInUser();

        if (user == null) {
            r.append("No user");
        } else {
            r.append("User Id: ").append(user.getId()).append(NL);
            
            var githubToken = tokens.getAccessToken(user.getId(), "github");
            r.append("GitHub Token: ").append(githubToken).append(NL);
            
            var eclipseToken = tokens.getAccessToken(user.getId(), "eclipse");
            r.append("Eclipse Token: ").append(eclipseToken).append(NL);
        }


        return r.toString();
    }

}