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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class UrlUtil {

    private UrlUtil() {
    }

    /**
     * Create a URL pointing to an API path.
     */
    public static String createApiUrl(String baseUrl, String... segments) {
        try {
            var result = new StringBuilder(baseUrl);
            for (var segment : segments) {
                if (segment == null)
                    return null;
                if (segment.isEmpty())
                    continue;
                if (result.length() == 0 || result.charAt(result.length() - 1) != '/')
                    result.append('/');
                result.append(URLEncoder.encode(segment, "UTF-8"));
            }
            return result.toString();
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Add a query to a URL. The parameters array must contain a sequence of key and value
     * pairs, so its length is expected to be even.
     */
    public static String addQuery(String url, String... parameters) {
        if (parameters.length % 2 != 0)
            throw new IllegalArgumentException("Expected an even number of parameters.");
        try {
            var result = new StringBuilder(url);
            var printedParams = 0;
            for (var i = 0; i < parameters.length; i += 2) {
                var key = parameters[i];
                var value = parameters[i + 1];
                if (key == null)
                    throw new NullPointerException("Parameter key must not be null");
                if (value != null) {
                    if (printedParams == 0)
                        result.append('?');
                    else
                        result.append('&');
                    result.append(key).append('=').append(URLEncoder.encode(value, "UTF-8"));
                    printedParams++;
                }
            }
            return result.toString();
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Get the base URL to use for API requests from the current servlet request.
     */
    public static String getBaseUrl() {
        var requestAttrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return getBaseUrl(requestAttrs.getRequest());
    }

    private static String getBaseUrl(HttpServletRequest request) {
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

    /**
     * Determine whether the given URL is absolute. A URL is regarded as absolute if
     * it specifies a host.
     */
    public static boolean isAbsolute(String urlString) {
        try {
            var url = new URL(urlString);
            return url.getHost() != null;
        } catch (MalformedURLException exc) {
            throw new RuntimeException(exc);
        }
    }

}