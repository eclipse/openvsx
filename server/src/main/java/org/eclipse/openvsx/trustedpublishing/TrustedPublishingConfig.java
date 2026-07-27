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
package org.eclipse.openvsx.trustedpublishing;

import java.time.Duration;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrustedPublishingConfig {
    /**
     * Whether trusted publishing is enabled at all.
     */
    @Value("${ovsx.trusted-publishing.enabled:false}")
    private boolean enabled;

    /**
     * The audience to expect in OIDC ID token; by default it is the URL of this instance frontend.
     */
    @Value("${ovsx.trusted-publishing.audience:${ovsx.webui.url:}}")
    private String audience;

    /**
     * Forbidden JWT headers that are enforced.
     * See <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core 1.0 - ID Token</a>
     */
    @Value("${ovsx.trusted-publishing.forbidden-jwt-headers:x5u,x5c,jku,jwk}")
    private List<String> forbiddenJwtHeaders;

    /**
     * The lifetime of access tokens issued in exchange for a valid OIDC ID token, in ISO-8601 duration format.
     */
    @Value("${ovsx.trusted-publishing.token-expiry:PT15M}")
    private String tokenExpiry;

    public boolean isEnabled() {
        return enabled;
    }

    @NonNull
    public String getAudience() {
        return audience;
    }

    @NonNull
    public List<String> getForbiddenJwtHeaders() {
        return forbiddenJwtHeaders;
    }

    @NonNull
    public Duration getTokenExpiry() {
        return Duration.parse(tokenExpiry);
    }

    @PostConstruct
    public void validate() {
        if (enabled) {
            if (audience == null || audience.isBlank()) {
                throw new IllegalStateException("Trusted publishing is enabled, but audience is not configured");
            }
            if (forbiddenJwtHeaders == null || forbiddenJwtHeaders.isEmpty()) {
                throw new IllegalStateException(
                        "Trusted publishing is enabled, but forbidden JWT headers are not configured");
            }
            Duration expiry = getTokenExpiry(); // throws if unparseable
            if (expiry.isNegative() || expiry.isZero()) {
                throw new IllegalStateException("Trusted publishing is enabled, but token expiry is not positive");
            }
        }
    }
}
