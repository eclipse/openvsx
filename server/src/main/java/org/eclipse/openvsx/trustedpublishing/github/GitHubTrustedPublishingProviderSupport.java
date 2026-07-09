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
package org.eclipse.openvsx.trustedpublishing.github;

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

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * GitHub specific support.
 *
 * @see <a href="https://docs.github.com/en/actions/concepts/security/openid-connect">GitHub OpenID Connect</a>
 */
public abstract class GitHubTrustedPublishingProviderSupport extends TrustedPublishingProviderSupport {
    private static final String CLAIM_REPOSITORY = "repository"; // "octo-org/octo-repo"
    private static final String CLAIM_REPOSITORY_ID = "repository_id"; // "74"
    private static final String CLAIM_REPOSITORY_OWNER = "repository_owner"; // "octo-org"
    private static final String CLAIM_REPOSITORY_OWNER_ID = "repository_owner_id"; // "65"
    private static final String CLAIM_ENVIRONMENT = "environment"; // "prod"; optional
    private static final String CLAIM_RUNNER_ENVIRONMENT = "runner_environment"; // "github-hosted"; for self-hosted GH runners this claim may not be included
    private static final String CLAIM_WORKFLOW_REF = "workflow_ref"; // "octo-org/octo-automation/.github/workflows/oidc.yml@refs/heads/main"

    private final String apiResolveRequest;

    protected GitHubTrustedPublishingProviderSupport(TrustedPublishingConfig config,
                                                     String providerId,
                                                     String providerName,
                                                     String providerUrl,
                                                     String oidcIssuer,
                                                     String apiResolveRequest) {
        super(config, providerId, providerName, providerUrl, oidcIssuer);
        this.apiResolveRequest = requireNonNull(apiResolveRequest);
    }

    @NonNull
    @Override
    protected Map<String, String> extractClaims(Jwt jwt) {
        requireNonNull(jwt);
        HashMap<String, String> result = new HashMap<>(7);
        mustClaim(jwt, JwtClaimNames.SUB, result);
        mustClaim(jwt, CLAIM_REPOSITORY, result);
        mustClaim(jwt, CLAIM_REPOSITORY_ID, result);
        mustClaim(jwt, CLAIM_REPOSITORY_OWNER, result);
        mustClaim(jwt, CLAIM_REPOSITORY_OWNER_ID, result);
        mayClaim(jwt, CLAIM_ENVIRONMENT, result);
        mayClaim(jwt, CLAIM_RUNNER_ENVIRONMENT, result);
        mustClaim(jwt, CLAIM_WORKFLOW_REF, result);
        return result;
    }

    @NonNull
    @Override
    protected Map<String, String> extractRequest(TrustRequest trustRequest) throws UnresolvableTrusteeException {
        requireNonNull(trustRequest);
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(apiResolveRequest, trustRequest.getOwner(), trustRequest.getRepo())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            throw new UnresolvableTrusteeException("Could not resolve GitHub repository "
                    + trustRequest.getOwner() + "/" + trustRequest.getRepo(), e);
        }
        if (response == null || !(response.get("id") instanceof Number repositoryId)
                || !(response.get("owner") instanceof Map<?, ?> owner)
                || !(owner.get("id") instanceof Number ownerId)) {
            throw new UnresolvableTrusteeException("Unexpected GitHub response for repository "
                    + trustRequest.getOwner() + "/" + trustRequest.getRepo());
        }

        HashMap<String, String> result = new HashMap<>();
        result.put(CLAIM_REPOSITORY, trustRequest.getOwner() + "/" + trustRequest.getRepo());
        result.put(CLAIM_REPOSITORY_ID, String.valueOf(repositoryId.longValue()));
        result.put(CLAIM_REPOSITORY_OWNER, trustRequest.getOwner());
        result.put(CLAIM_REPOSITORY_OWNER_ID, String.valueOf(ownerId.longValue()));
        // registered without the "@<ref>" part: publishing is trusted regardless of branch or tag
        result.put(CLAIM_WORKFLOW_REF, trustRequest.getOwner() + "/" + trustRequest.getRepo()
                + "/.github/workflows/" + trustRequest.getWorkflow());
        trustRequest.getEnvironment().ifPresent(env -> result.put(CLAIM_ENVIRONMENT, env));
        return result;
    }

    @Override
    public boolean matches(@NonNull Map<String, String> registered, @NonNull Map<String, String> token) {
        requireNonNull(registered);
        requireNonNull(token);
        return claimEquals(CLAIM_REPOSITORY_ID, registered, token)
                && claimEquals(CLAIM_REPOSITORY_OWNER_ID, registered, token)
                && registered.get(CLAIM_WORKFLOW_REF) != null
                && registered.get(CLAIM_WORKFLOW_REF).equals(stripRef(token.get(CLAIM_WORKFLOW_REF)))
                && pinnedClaimMatches(CLAIM_ENVIRONMENT, registered, token);
    }
}
