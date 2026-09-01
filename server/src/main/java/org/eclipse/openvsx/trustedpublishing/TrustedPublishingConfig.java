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

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.eclipse.openvsx.trustedpublishing.TrustedPublishingProperties.GitLabInstance;
import org.eclipse.openvsx.trustedpublishing.github.GitHubTrustedPublishingProvider;

@Configuration
@EnableConfigurationProperties(TrustedPublishingProperties.class)
public class TrustedPublishingConfig {

    private final TrustedPublishingProperties properties;

    public TrustedPublishingConfig(TrustedPublishingProperties properties) {
        this.properties = properties;
    }

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
     * The comma separated list of active trusted publishing providers. An id listed here must be
     * {@code github} or one of the configured GitLab instances, see {@link TrustedPublishingProperties}.
     * Default: {@code github}.
     */
    @Value("${ovsx.trusted-publishing.active-providers:github}")
    private List<String> activeProviders;

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
    public List<String> getActiveProviders() {
        return activeProviders;
    }

    /**
     * The configured GitLab instances, keyed by provider id. Whether an instance can actually be used
     * is decided by {@link #getActiveProviders()}.
     */
    @NonNull
    public Map<String, GitLabInstance> getGitLabInstances() {
        return properties.getGitlab();
    }

    /**
     * How long an issued publishing token is valid, {@code ovsx.trusted-publishing.token-expiration}.
     */
    @NonNull
    public Duration getTokenExpiration() {
        return properties.getTokenExpiration();
    }

    @PostConstruct
    public void validate() {
        // checked whether or not the feature is on, so a typo does not lie in wait until it is turned on
        var tokenExpiration = getTokenExpiration();
        if (tokenExpiration == null || !tokenExpiration.isPositive()) {
            throw new IllegalStateException(
                    "ovsx.trusted-publishing.token-expiration must be a positive duration, got: " + tokenExpiration);
        }
        if (enabled) {
            if (audience == null || audience.isBlank()) {
                throw new IllegalStateException("Trusted publishing is enabled, but audience is not configured");
            }
            if (forbiddenJwtHeaders == null || forbiddenJwtHeaders.isEmpty()) {
                throw new IllegalStateException(
                        "Trusted publishing is enabled, but forbidden JWT headers are not configured");
            }
            if (activeProviders == null || activeProviders.isEmpty()) {
                throw new IllegalStateException(
                        "Trusted publishing is enabled, but there are no active providers configured");
            }
            validateGitLabInstances();
        }
    }

    private void validateGitLabInstances() {
        for (var entry : getGitLabInstances().entrySet()) {
            var id = entry.getKey();
            var instance = entry.getValue();
            if (GitHubTrustedPublishingProvider.PROVIDER_ID.equals(id)) {
                throw new IllegalStateException(
                        "GitLab instance '" + id + "' uses the provider id of the GitHub provider");
            }
            // a configured instance replaces a default one as a whole, so it must carry every field itself
            if (instance.getName() == null || instance.getName().isBlank()
                    || instance.getUrl() == null || instance.getUrl().isBlank()) {
                throw new IllegalStateException("GitLab instance '" + id + "' has no name or no URL configured");
            }
            for (var url : List.of(instance.getUrl(), instance.getIssuer())) {
                if (hostOf(url) == null) {
                    throw new IllegalStateException("GitLab instance '" + id + "' has a malformed URL: " + url);
                }
            }
        }
    }

    @Nullable
    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException exc) {
            return null;
        }
    }
}
