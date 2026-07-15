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
package org.eclipse.openvsx.trustedpublishing.gitlab;

import org.eclipse.openvsx.trustedpublishing.TrustRequest;
import org.eclipse.openvsx.trustedpublishing.TrustedPublishingProviderSupport;
import org.eclipse.openvsx.trustedpublishing.TrustedPublishingConfig;
import org.eclipse.openvsx.trustedpublishing.UnresolvableTrusteeException;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * GitLab specific support.
 *
 * @see <a href="https://docs.gitlab.com/ci/secrets/id_token_authentication/">GitLab OpenID Connect</a>
 */
public abstract class GitLabTrustedPublishingProviderSupport extends TrustedPublishingProviderSupport {
    private static final String CLAIM_NAMESPACE_ID = "namespace_id"; // "72"
    private static final String CLAIM_NAMESPACE_PATH = "namespace_path"; // "my-group"
    private static final String CLAIM_PROJECT_ID = "project_id"; // "20"
    private static final String CLAIM_PROJECT_PATH = "project_path"; // "my-group/my-project"
    private static final String CLAIM_ENVIRONMENT = "environment"; // "prod"; optional
    private static final String CLAIM_RUNNER_ENVIRONMENT = "runner_environment"; // "gitlab-hosted"
    private static final String CLAIM_CI_CONFIG_REF_URI = "ci_config_ref_uri"; // "gitlab.example.com/my-group/my-project//.gitlab-ci.yml@refs/heads/main"

    private static final String API_RESOLVE_REQUEST = "/api/v4/projects/{path}";

    protected GitLabTrustedPublishingProviderSupport(TrustedPublishingConfig config,
                                                     String providerId,
                                                     String providerName,
                                                     String providerUrl,
                                                     String oidcIssuer) {
        super(config, providerId, providerName, providerUrl, oidcIssuer);
    }

    @NonNull
    @Override
    protected Map<String, String> extractClaims(Jwt jwt) {
        requireNonNull(jwt);
        HashMap<String, String> result = new HashMap<>(7);
        mustClaim(jwt, JwtClaimNames.SUB, result);
        mustClaim(jwt, CLAIM_NAMESPACE_ID, result);
        mustClaim(jwt, CLAIM_NAMESPACE_PATH, result);
        mustClaim(jwt, CLAIM_PROJECT_ID, result);
        mustClaim(jwt, CLAIM_PROJECT_PATH, result);
        mayClaim(jwt, CLAIM_ENVIRONMENT, result);
        mustClaim(jwt, CLAIM_RUNNER_ENVIRONMENT, result);
        mustClaim(jwt, CLAIM_CI_CONFIG_REF_URI, result);
        return result;
    }

    @NonNull
    @Override
    protected Map<String, String> extractRequest(TrustRequest trustRequest) throws UnresolvableTrusteeException {
        requireNonNull(trustRequest);
        String projectPath = trustRequest.getOwner() + "/" + trustRequest.getRepo();
        Map<String, Object> response = resolve(trustRequest);
        if (response == null || !(response.get("id") instanceof Number projectId)
                || !(response.get("namespace") instanceof Map<?, ?> namespace)
                || !(namespace.get("id") instanceof Number namespaceId)) {
            throw new UnresolvableTrusteeException("Unexpected GitLab response for project " + projectPath);
        }

        HashMap<String, String> result = new HashMap<>();
        result.put(CLAIM_NAMESPACE_ID, String.valueOf(namespaceId.longValue()));
        result.put(CLAIM_NAMESPACE_PATH, trustRequest.getOwner());
        result.put(CLAIM_PROJECT_ID, String.valueOf(projectId.longValue()));
        result.put(CLAIM_PROJECT_PATH, projectPath);
        // registered without the "@<ref>" part: publishing is trusted regardless of branch or tag
        result.put(CLAIM_CI_CONFIG_REF_URI, URI.create(providerUrl).getHost() + "/" + projectPath
                + "//" + trustRequest.getWorkflow());
        trustRequest.getEnvironment().ifPresent(env -> result.put(CLAIM_ENVIRONMENT, env));
        return result;
    }

    /**
     * Pulled out for testability; is mocked in UT to prevent real remote access.
     */
    protected Map<String, Object> resolve(TrustRequest trustRequest) {
        String projectPath = trustRequest.getOwner() + "/" + trustRequest.getRepo();
        try {
            // the {path} template variable is URL-encoded by RestClient, turning "/" into "%2F" as GitLab expects
            return restClient.get()
                    .uri(providerUrl + API_RESOLVE_REQUEST, projectPath)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            throw new UnresolvableTrusteeException("Could not resolve GitLab project " + projectPath, e);
        }
    }

    @Override
    public boolean matches(@NonNull Map<String, String> registered, @NonNull Map<String, String> token) {
        requireNonNull(registered);
        requireNonNull(token);
        return claimEquals(CLAIM_PROJECT_ID, registered, token)
                && claimEquals(CLAIM_NAMESPACE_ID, registered, token)
                && registered.get(CLAIM_CI_CONFIG_REF_URI) != null
                && registered.get(CLAIM_CI_CONFIG_REF_URI).equals(stripRef(token.get(CLAIM_CI_CONFIG_REF_URI)))
                && pinnedClaimMatches(CLAIM_ENVIRONMENT, registered, token);
    }
}
