/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class ShallowEtagHeaderFilter extends org.springframework.web.filter.ShallowEtagHeaderFilter {

    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // limit the filter to /api/{namespace}/{extension} and /api/{namespace}/{extension}/{version} endpoints
        var path = request.getRequestURI().substring(1).split("/");
        var applyFilter = path.length == 3 || path.length == 4;
        if(applyFilter) {
            applyFilter = path[0].equals("api") && !path[1].equals("-");
        }
        if(applyFilter && path.length == 4) {
            applyFilter = !(path[3].equals("review") || path[3].equals("reviews"));
        }

        return !applyFilter;
    }
}
