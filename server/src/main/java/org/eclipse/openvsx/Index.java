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
            r.append("No user").append(NL);
        } else {
            r.append("User Id: ").append(user.getId()).append(NL);
            
            var githubToken = tokens.getActiveToken(user, "github");
            if (githubToken == null)
                r.append("No GitHub token").append(NL);
            else
                r.append("GitHub token: ").append(githubToken.accessToken).append(NL);
            
            var eclipseToken = tokens.getActiveToken(user, "eclipse");
            if (eclipseToken == null)
                r.append("No Eclipse token").append(NL);
            else
                r.append("Eclipse token: ").append(eclipseToken.accessToken).append(NL);
        }


        return r.toString();
    }

}