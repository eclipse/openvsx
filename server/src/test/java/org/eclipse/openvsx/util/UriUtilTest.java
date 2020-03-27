/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UriUtilTest {

    @Test
    public void testApiUrl() throws Exception {
        var baseUrl = "http://localhost/";
        assertEquals("http://localhost/api/foo/b%09a%2Fr",
                UrlUtil.createApiUrl(baseUrl, "api", "foo", "b\ta/r"));
    }

    @Test
    public void testQuery() throws Exception {
        var url = "http://localhost/api/foo";
        assertEquals("http://localhost/api/foo?a=1&c=b%09a%2Fr",
                UrlUtil.addQuery(url, "a", "1", "b", null, "c", "b\ta/r"));
    }

}