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

}