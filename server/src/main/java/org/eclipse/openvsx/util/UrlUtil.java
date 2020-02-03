/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

public final class UrlUtil {

    private UrlUtil() {}

    /**
     * Create a URL pointing to an API path.
     */
    public static String createApiUrl(String baseUrl, String... segments) {
        try {
            var result = new StringBuilder(baseUrl);
            if (!baseUrl.endsWith("/"))
                result.append("/");
            result.append("api");
            for (var segment : segments) {
                if (segment == null)
                    return null;
				result.append('/').append(URLEncoder.encode(segment, "UTF-8"));
            }
            return result.toString();
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Get the base URL to use for API requests from the given servlet request.
     */
    public static String getBaseUrl(HttpServletRequest request) {
        // FIXME workaround for port forwarding in Gitpod
        var scheme = "https"; //request.getScheme();
        var port = 443; //request.getServerPort();
        var url = new StringBuilder();
        url.append(scheme).append("://").append(request.getServerName());
        switch (scheme) {
            case "http":
                if (port != 80)
                    url.append(":").append(port);
                break;
            case "https":
                if (port != 443)
                    url.append(":").append(port);
                break;
        }
        url.append(request.getContextPath());
        return url.toString();
    }

}