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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import org.eclipse.openvsx.trustedpublishing.gitlab.GitLabTrustedPublishingProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * The GitLab instances are configuration rather than code, so what matters is that the public instance
 * is there, that configured instances are added to it, and that a broken instance is caught at startup.
 */
class TrustedPublishingPropertiesTest {

    @Test
    void onlyThePublicInstanceIsConfiguredByDefault() {
        var instances = new TrustedPublishingProperties().getGitlab();

        assertThat(instances).containsOnlyKeys(GitLabTrustedPublishingProvider.PROVIDER_ID);
        var gitlab = instances.get(GitLabTrustedPublishingProvider.PROVIDER_ID);
        assertThat(gitlab.getName()).isEqualTo("GitLab");
        assertThat(gitlab.getUrl()).isEqualTo(GitLabTrustedPublishingProvider.PROVIDER_URL);
        // GitLab issues its tokens under its own base URL
        assertThat(gitlab.getIssuer()).isEqualTo(GitLabTrustedPublishingProvider.PROVIDER_URL);
    }

    @Test
    void configuredInstanceIsAddedToThePublicOne() {
        var instances = bind(
                Map.of(
                        "ovsx.trusted-publishing.gitlab.eclipse-gitlab.name",
                        "Eclipse GitLab",
                        "ovsx.trusted-publishing.gitlab.eclipse-gitlab.url",
                        "https://gitlab.eclipse.org"))
                .getGitlab();

        assertThat(instances).containsOnlyKeys(GitLabTrustedPublishingProvider.PROVIDER_ID, "eclipse-gitlab");
        var eclipse = instances.get("eclipse-gitlab");
        assertThat(eclipse.getName()).isEqualTo("Eclipse GitLab");
        assertThat(eclipse.getIssuer()).isEqualTo("https://gitlab.eclipse.org");
    }

    @Test
    void configuredInstanceCanRedefineThePublicOne() {
        var instances = bind(
                Map.of(
                        "ovsx.trusted-publishing.gitlab.gitlab.name",
                        "GitLab (staging)",
                        "ovsx.trusted-publishing.gitlab.gitlab.url",
                        "https://gitlab.staging.example",
                        "ovsx.trusted-publishing.gitlab.gitlab.issuer",
                        "https://issuer.staging.example"))
                .getGitlab();

        var gitlab = instances.get(GitLabTrustedPublishingProvider.PROVIDER_ID);
        assertThat(gitlab.getName()).isEqualTo("GitLab (staging)");
        assertThat(gitlab.getUrl()).isEqualTo("https://gitlab.staging.example");
        assertThat(gitlab.getIssuer()).isEqualTo("https://issuer.staging.example");
    }

    @Test
    void redefiningTheDefaultInstanceReplacesItAsAWhole() {
        // only the URL is given, so the default name is gone rather than kept - and startup says so
        var properties = bind(
                Map.of("ovsx.trusted-publishing.gitlab.gitlab.url", "https://gitlab.staging.example"));

        assertThat(properties.getGitlab().get(GitLabTrustedPublishingProvider.PROVIDER_ID).getName()).isNull();
        assertThatIllegalStateException().isThrownBy(() -> enabledConfig(properties).validate())
                .withMessageContaining("no name or no URL");
    }

    @Test
    void enabledConfigAcceptsTheDefaultInstance() {
        assertThatCode(() -> enabledConfig(new TrustedPublishingProperties()).validate()).doesNotThrowAnyException();
    }

    @Test
    void enabledConfigRejectsAnInstanceTakingTheGitHubProviderId() {
        var properties = bind(
                Map.of(
                        "ovsx.trusted-publishing.gitlab.github.name",
                        "Not GitHub",
                        "ovsx.trusted-publishing.gitlab.github.url",
                        "https://gitlab.acme.example"));

        assertThatIllegalStateException().isThrownBy(() -> enabledConfig(properties).validate())
                .withMessageContaining("provider id of the GitHub provider");
    }

    @Test
    void enabledConfigRejectsAMalformedInstanceUrl() {
        var properties = bind(
                Map.of(
                        "ovsx.trusted-publishing.gitlab.acme-gitlab.name",
                        "ACME GitLab",
                        "ovsx.trusted-publishing.gitlab.acme-gitlab.url",
                        "gitlab.acme.example"));

        assertThatIllegalStateException().isThrownBy(() -> enabledConfig(properties).validate())
                .withMessageContaining("malformed URL");
    }

    private static TrustedPublishingProperties bind(Map<String, Object> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind(
                        "ovsx.trusted-publishing",
                        Bindable.ofInstance(new TrustedPublishingProperties()))
                .orElseGet(TrustedPublishingProperties::new);
    }

    private static TrustedPublishingConfig enabledConfig(TrustedPublishingProperties properties) {
        var config = new TrustedPublishingConfig(properties);
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "audience", "https://open-vsx.org");
        ReflectionTestUtils.setField(config, "forbiddenJwtHeaders", List.of("x5u"));
        ReflectionTestUtils.setField(config, "activeProviders", List.of("github"));
        return config;
    }
}
