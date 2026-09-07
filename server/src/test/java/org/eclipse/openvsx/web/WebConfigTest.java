/********************************************************************************
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
 ********************************************************************************/
package org.eclipse.openvsx.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.Cookie;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CORS mappings this application registers, and which of them depend on where the Web UI lives.
 * <p>
 * Asserted against the registry rather than over HTTP because registration <em>order</em> is part of the
 * behaviour: {@code UrlBasedCorsConfigurationSource} returns the configuration of the first pattern that
 * matches a request and stops looking, so what the map holds and the order it holds it in are together
 * the whole of the policy.
 */
class WebConfigTest {

    private static final String UI_ORIGIN = "https://ui.example.com";

    /** {@link CorsRegistry#getCorsConfigurations()} is protected, and this test is what wants to read it. */
    private static class ReadableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    private static Map<String, CorsConfiguration> mappingsFor(String webuiUrl) {
        var config = new WebConfig(Optional.empty());
        config.webuiUrl = webuiUrl;
        var registry = new ReadableCorsRegistry();
        config.addCorsMappings(registry);
        return registry.configurations();
    }

    // The registry API, the VS Code gallery adapter and the static documents are public, and the browser
    // clients that read them - vscode.dev, Gitpod, anything querying the gallery from page script - need
    // these headers whether or not the Web UI happens to sit on its own origin. Gating them on that left
    // a same-origin deployment serving none at all.
    @Test
    void offersThePublicApiToEveryOriginWithoutAWebUiUrl() {
        var mappings = mappingsFor("");

        assertThat(mappings).containsOnlyKeys("/api/**", "/vscode/**", "/documents/**");
        assertThat(mappings.values())
                .allSatisfy(config -> {
                    assertThat(config.getAllowedOrigins()).containsExactly("*");
                    // Never credentials on a wildcard origin: a browser refuses the combination, and it is
                    // the combination that would make any origin an authenticated reader.
                    assertThat(config.getAllowCredentials()).isNull();
                });
    }

    // A relative value names no origin, so there is no cross-origin Web UI to allow.
    @Test
    void treatsARelativeWebUiUrlAsNoSeparateOrigin() {
        assertThat(mappingsFor("/")).containsOnlyKeys("/api/**", "/vscode/**", "/documents/**");
    }

    @Test
    void allowsTheWebUiOriginWithCredentialsWhenItIsAbsolute() {
        var mappings = mappingsFor(UI_ORIGIN);

        assertThat(mappings).containsKey("/user/**");
        var userMapping = mappings.get("/user/**");
        assertThat(userMapping.getAllowedOrigins()).containsExactly(UI_ORIGIN);
        assertThat(userMapping.getAllowCredentials()).isTrue();
    }

    @Test
    void neverAllowsCredentialsFromAnOriginItWasNotGiven() {
        var mappings = mappingsFor(UI_ORIGIN);

        assertThat(mappings)
                .allSatisfy((pattern, config) -> {
                    if (Boolean.TRUE.equals(config.getAllowCredentials())) {
                        assertThat(config.getAllowedOrigins()).containsExactly(UI_ORIGIN);
                        assertThat(config.checkOrigin("https://attacker.example")).isNull();
                    }
                });
    }

    /**
     * The trap in hoisting the public mappings: {@code /api/user/publish} and the other credentialed
     * {@code /api} endpoints are all matched by {@code /api/**} as well, and the first pattern registered
     * is the one that answers. Registered the other way round they would quietly serve the public,
     * credential-less configuration instead, and nothing about the request would look wrong.
     */
    @Test
    void registersTheCredentialedApiEndpointsAheadOfTheCatchAll() {
        var patterns = List.copyOf(mappingsFor(UI_ORIGIN).keySet());

        assertThat(patterns).contains("/api/**");
        var catchAll = patterns.indexOf("/api/**");
        assertThat(patterns.subList(0, catchAll))
                .as("credentialed /api mappings must be registered before the catch-all that also matches them")
                .contains(
                        "/api/user/publish",
                        "/api/user/namespace/create",
                        "/api/*/*/review/**",
                        "/api/-/trusted-publishing/status");
    }

    // Left to the browser, an unmarked cookie is Lax in Chromium and has not always been elsewhere. It is
    // what stops a cross-site fetch from carrying the session, so it is stated rather than assumed.
    @Test
    void marksCookiesLaxRatherThanLeavingItToTheBrowser() {
        var supplier = new WebConfig(Optional.empty()).laxCookieSameSiteSupplier();

        assertThat(supplier.getSameSite(new jakarta.servlet.http.Cookie("JSESSIONID", "value")))
                .isEqualTo(Cookie.SameSite.LAX);
    }
}
