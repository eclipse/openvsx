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
        var url = new StringBuilder();

        // Use the scheme from the X-Forwarded-Proto header if present
        String scheme;
        var forwardedScheme = request.getHeader("X-Forwarded-Proto");
        if (forwardedScheme == null) {
            scheme = request.getScheme();
        } else {
            scheme = forwardedScheme;
        }
        url.append(scheme).append("://");

        // Use the host and port from the X-Forwarded-Host header if present
        String host;
        int port;
        var forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost == null) {
            host = request.getServerName();
            port = request.getServerPort();
        } else {
            int colonIndex = forwardedHost.lastIndexOf(':');
            if (colonIndex > 0) {
                host = forwardedHost.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(forwardedHost.substring(colonIndex + 1));
                } catch (NumberFormatException exc) {
                    port = -1;
                }
            } else {
                host = forwardedHost;
                port = -1;
            }
        }
        url.append(host);
        switch (scheme) {
            case "http":
                if (port != 80 && port > 0)
                    url.append(":").append(port);
                break;
            case "https":
                if (port != 443 && port > 0)
                    url.append(":").append(port);
                break;
        }

        url.append(request.getContextPath());
        return url.toString();
    }

}