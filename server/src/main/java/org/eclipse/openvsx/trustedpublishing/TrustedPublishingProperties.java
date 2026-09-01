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

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import org.eclipse.openvsx.trustedpublishing.gitlab.GitLabTrustedPublishingProvider;

/**
 * The trusted publishing provider instances that are known to this registry.
 * <p>
 * Every GitLab instance behaves the same way, it only differs in its id, name, URL and OIDC issuer,
 * so instances are configured rather than coded. Only the public instance is configured out of the box;
 * any other one - the Eclipse Foundation instance included - is added by configuration, and becomes
 * usable once its id is listed in {@code ovsx.trusted-publishing.active-providers}:
 *
 * <pre>
 * ovsx:
 *   trusted-publishing:
 *     active-providers: github,eclipse-gitlab
 *     gitlab:
 *       eclipse-gitlab:
 *         name: Eclipse GitLab
 *         url: https://gitlab.eclipse.org
 *         issuer: https://gitlab.eclipse.org   # optional, defaults to the URL
 * </pre>
 *
 * The id is persisted with every registration, so renaming it hides the registrations made for it.
 * <p>
 * Configuring the id of the default instance replaces it as a whole rather than patching single fields,
 * so such an entry has to carry the name and the URL itself.
 */
@ConfigurationProperties(prefix = "ovsx.trusted-publishing")
@Validated
public class TrustedPublishingProperties {

    @Valid
    private Map<String, GitLabInstance> gitlab = defaultGitLabInstances();

    /**
     * The known GitLab instances, keyed by provider id. Configured instances are added to the public
     * instance, which stays available unless its id is redefined.
     */
    @NonNull
    public Map<String, GitLabInstance> getGitlab() {
        return gitlab;
    }

    public void setGitlab(Map<String, GitLabInstance> gitlab) {
        this.gitlab = gitlab;
    }

    private static Map<String, GitLabInstance> defaultGitLabInstances() {
        var instances = new LinkedHashMap<String, GitLabInstance>();
        instances.put(
                GitLabTrustedPublishingProvider.PROVIDER_ID,
                new GitLabInstance("GitLab", GitLabTrustedPublishingProvider.PROVIDER_URL));
        return instances;
    }

    /**
     * A single GitLab instance.
     */
    public static class GitLabInstance {

        @NotBlank
        private String name;

        @NotBlank
        private String url;

        @Nullable
        private String issuer;

        public GitLabInstance() {
            // for configuration property binding
        }

        public GitLabInstance(String name, String url) {
            this.name = name;
            this.url = url;
        }

        /**
         * The instance name, for human consumption.
         */
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        /**
         * The base URL of the instance; the API and the {@code ci_config_ref_uri} claim are derived from it.
         */
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * The issuer to expect in the {@code iss} claim of issued OIDC ID tokens. GitLab issues tokens
         * under its own base URL, so this defaults to {@link #getUrl()}.
         */
        public String getIssuer() {
            return issuer == null || issuer.isBlank() ? url : issuer;
        }

        public void setIssuer(@Nullable String issuer) {
            this.issuer = issuer;
        }
    }
}
