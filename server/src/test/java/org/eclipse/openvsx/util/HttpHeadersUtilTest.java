/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.util;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpHeadersUtilTest {

    @Test
    void testCreateJsonFileResponseHeaders() {
        var headers = HttpHeadersUtil.createJsonFileResponseHeaders();

        var contentType = headers.getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).isEqualTo("application/json");

        var contentTypeOptions = headers.get("X-Content-Type-Options");
        assertThat(contentTypeOptions).isEqualTo(List.of("nosniff"));

        var contentSecurityPolicy = headers.get("Content-Security-Policy");
        assertThat(contentSecurityPolicy).isEqualTo(
                List.of("default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; sandbox")
        );
    }

    @Test
    void testCreateFileResponseHeadersForHtml() {
        var headers = HttpHeadersUtil.createFileResponseHeaders((InputStream) null, "file.html");

        var contentType = headers.getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).isEqualTo("text/plain;charset=UTF-8");

        var contentTypeOptions = headers.get("X-Content-Type-Options");
        assertThat(contentTypeOptions).isEqualTo(List.of("nosniff"));

        var contentSecurityPolicy = headers.get("Content-Security-Policy");
        assertThat(contentSecurityPolicy).isEqualTo(
                List.of("default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; sandbox")
        );
    }

    @Test
    void testCreateFileResponseHeadersForVsix() {
        var headers = HttpHeadersUtil.createFileResponseHeaders((InputStream) null, "file.vsix");

        var contentType = headers.getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).isEqualTo("application/octet-stream");

        var contentTypeOptions = headers.get("X-Content-Type-Options");
        assertThat(contentTypeOptions).isEqualTo(List.of("nosniff"));

        var contentSecurityPolicy = headers.get("Content-Security-Policy");
        assertThat(contentSecurityPolicy).isEqualTo(
                List.of("default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; sandbox")
        );

        var contentDisposition = headers.getContentDisposition();
        assertThat(contentDisposition.isAttachment()).isTrue();
        assertThat(contentDisposition.getFilename()).isEqualTo("file.vsix");
    }
}
