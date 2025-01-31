/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;

import org.eclipse.openvsx.OVSXConfig;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.AuthUserFactory;
import org.eclipse.openvsx.security.OAuth2UserServices;
import org.eclipse.openvsx.security.SecurityConfig;
import org.eclipse.openvsx.security.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SitemapController.class)
@AutoConfigureWebClient
@MockBean({
        EclipseService.class, SimpleMeterRegistry.class, UserService.class, TokenService.class, EntityManager.class
})
class SitemapControllerTest {

    @MockBean
    RepositoryService repositories;

    @Autowired
    MockMvc mockMvc;

    @Test
    void testSitemap() throws Exception {
        var expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url>
                    <loc>http://localhost/extension/EditorConfig/EditorConfig</loc>
                    <lastmod>2024-04-10</lastmod>
                  </url>
                </urlset>
                """;

        var rows = List.of(new SitemapRow("EditorConfig", "EditorConfig", "2024-04-10"));
        Mockito.when(repositories.fetchSitemapRows()).thenReturn(rows);
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().xml(expected));
    }

    @TestConfiguration
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        OAuth2UserServices oauth2UserServices(
                UserService users,
                TokenService tokens,
                RepositoryService repositories,
                EntityManager entityManager,
                EclipseService eclipse,
                AuthUserFactory authUserFactory
        ) {
            return new OAuth2UserServices(users, tokens, repositories, entityManager, eclipse, authUserFactory);
        }

        @Bean
        SitemapService sitemapService(RepositoryService repositories) {
            return new SitemapService(repositories);
        }

        @Bean
        AuthUserFactory authUserFactory(
            OVSXConfig config
        ) {
            return new AuthUserFactory(config);
        }

        @Bean
        OVSXConfig ovsxConfig() {
            return new OVSXConfig();
        }
    }
}
