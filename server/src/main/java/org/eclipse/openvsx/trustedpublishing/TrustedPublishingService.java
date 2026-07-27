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

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.nimbusds.jwt.JWTParser;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.stereotype.Service;

import org.eclipse.openvsx.accesstoken.AccessTokenService;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.TrustedPublisher;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.trustedpublishing.github.GitHubTrustedPublishingProvider;
import org.eclipse.openvsx.trustedpublishing.gitlab.EclipseGitLabTrustedPublishingProvider;
import org.eclipse.openvsx.trustedpublishing.gitlab.GitLabTrustedPublishingProvider;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TimeUtil;

import static java.util.Objects.requireNonNull;

@Service
public class TrustedPublishingService {
    private static final int TOKEN_DESCRIPTION_SIZE = 255;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TrustedPublishingConfig config;
    private final RepositoryService repositories;
    private final AccessTokenService tokens;
    private final EntityManager entityManager;

    private final Map<String, TrustedPublishingProviderSupport> providers;

    public TrustedPublishingService(
            TrustedPublishingConfig config,
            RepositoryService repositories,
            AccessTokenService tokens,
            EntityManager entityManager
    ) {
        this.config = requireNonNull(config);
        this.repositories = requireNonNull(repositories);
        this.tokens = requireNonNull(tokens);
        this.entityManager = requireNonNull(entityManager);

        if (config.isEnabled()) {
            this.providers = Map.of(
                    GitHubTrustedPublishingProvider.PROVIDER_ID,
                    new GitHubTrustedPublishingProvider(config),
                    GitLabTrustedPublishingProvider.PROVIDER_ID,
                    new GitLabTrustedPublishingProvider(config),
                    EclipseGitLabTrustedPublishingProvider.PROVIDER_ID,
                    new EclipseGitLabTrustedPublishingProvider(config));
        } else {
            this.providers = Map.of();
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    private void ensureEnabled() {
        if (!config.isEnabled()) {
            throw new ErrorResultException("Trusted publishing is not enabled.", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Client initiated registration of trusted publishing. The user must be an owner of the namespace.
     */
    @Transactional
    public TrustedPublisher registerTrustedPublisher(
            UserData user,
            String namespaceName,
            String extensionName,
            String providerId,
            Map<String, String> registration
    ) {
        requireNonNull(user);
        requireNonNull(namespaceName);
        requireNonNull(extensionName);
        requireNonNull(providerId);
        requireNonNull(registration);
        ensureEnabled();
        TrustedPublishingProviderSupport provider = providers.get(providerId);
        if (provider == null) {
            throw new ErrorResultException("Unknown trusted publishing provider: " + providerId);
        }
        Namespace namespace = requireOwnedNamespace(user, namespaceName);

        Map<String, String> claims = provider.extractRequest(registration);

        boolean duplicate = repositories.findTrustedPublishers(namespace).stream()
                .anyMatch(
                        tp -> Objects.equals(extensionName, tp.getExtensionName())
                                && tp.getProvider().equals(provider.getProviderId())
                                && tp.getClaims().equals(claims));
        if (duplicate) {
            throw new ErrorResultException("An equivalent trusted publisher is already registered.");
        }

        TrustedPublisher publisher = new TrustedPublisher();
        publisher.setNamespace(namespace);
        publisher.setExtensionName(extensionName);
        publisher.setProvider(provider.getProviderId());
        publisher.setRegistration(registration);
        publisher.setClaims(claims);
        publisher.setCreatedBy(entityManager.merge(user));
        publisher.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        entityManager.persist(publisher);
        return publisher;
    }

    /**
     * Lists trusted publishers of a namespace. The user must be an owner of the namespace.
     */
    public List<TrustedPublisher> getTrustedPublishers(UserData user, String namespaceName) {
        requireNonNull(user);
        requireNonNull(namespaceName);
        ensureEnabled();
        Namespace namespace = requireOwnedNamespace(user, namespaceName);
        return repositories.findTrustedPublishers(namespace).toList();
    }

    /**
     * Deletes a trusted publisher registration. The user must be an owner of the namespace.
     */
    @Transactional
    public ResultJson deleteTrustedPublisher(UserData user, String namespaceName, long id) {
        requireNonNull(user);
        requireNonNull(namespaceName);
        ensureEnabled();
        Namespace namespace = requireOwnedNamespace(user, namespaceName);
        TrustedPublisher publisher = repositories.findTrustedPublisher(id);
        if (publisher == null || publisher.getNamespace().getId() != namespace.getId()) {
            throw new NotFoundException();
        }
        repositories.deleteTrustedPublisher(publisher);
        return ResultJson.success("Deleted trusted publisher for namespace " + namespace.getName() + ".");
    }

    /**
     * Lists trusted publisher providers supported on a namespace. The user must be an owner of the namespace.
     * For now, we do not use any of the provided information to filter providers, just enforce required conditions.
     */
    public Map<String, TrustedPublishingProviderSupport> getTrustedPublisherProviders(
            UserData user,
            String namespaceName
    ) {
        requireNonNull(user);
        requireNonNull(namespaceName);
        ensureEnabled();
        requireOwnedNamespace(user, namespaceName);
        return providers;
    }

    /**
     * Client signaled publishing intent, by submitting OIDC ID token. If publishing intent is approved,
     * service will issue an access token that is returned to client, and client should publish using received
     * token.
     */
    @Transactional
    public AccessTokenJson requestPublishToken(String namespaceName, String extensionName, String token) {
        requireNonNull(namespaceName);
        requireNonNull(extensionName);
        requireNonNull(token);
        ensureEnabled();

        // just blindly parse token to get "iss" claim from it; to identify provider to use
        String issuer;
        try {
            Object iss = JWTParser.parse(token).getJWTClaimsSet().getClaim(JwtClaimNames.ISS);
            if (iss == null) {
                throw new ErrorResultException("Token does not have issuer set.");
            }
            issuer = iss instanceof String ? (String) iss : String.valueOf(iss);
        } catch (ParseException e) {
            throw new ErrorResultException("Failed to pre-parse token.");
        }

        // select provider based on "iss"
        TrustedPublishingProviderSupport provider = providers.values().stream()
                .filter(p -> Objects.equals(issuer, p.getOidcIssuer()))
                .findFirst()
                .orElseThrow(() -> new ErrorResultException("Unsupported token issuer."));

        // using provider validate token and extract claims of interest
        Map<String, String> claims = provider.extract(token)
                .orElseThrow(() -> new ErrorResultException("The token could not be validated.", HttpStatus.FORBIDDEN));

        Namespace namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new ErrorResultException("No trusted publisher matches the presented token.", HttpStatus.FORBIDDEN);
        }

        TrustedPublisher match = repositories.findTrustedPublishers(namespace).stream()
                .filter(tp -> Objects.equals(extensionName, tp.getExtensionName()))
                .filter(tp -> tp.getProvider().equals(provider.getProviderId()))
                .filter(tp -> provider.matches(tp.getClaims(), claims))
                .findFirst()
                .orElseThrow(
                        () -> new ErrorResultException(
                                "No trusted publisher matches the presented token.",
                                HttpStatus.FORBIDDEN));

        // ponytail: the issued token is a regular personal access token of the registering user, valid for
        // any namespace that user can publish to; add namespace-scoped tokens if broader scope becomes a concern
        String description = "Trusted publishing (" + provider.getProviderId() + "): " + claims.get(JwtClaimNames.SUB);
        if (description.length() > TOKEN_DESCRIPTION_SIZE) {
            description = description.substring(0, TOKEN_DESCRIPTION_SIZE);
        }
        logger.info(
                "Issuing trusted publishing token for namespace {} to {}",
                namespace.getName(),
                claims.get(JwtClaimNames.SUB));
        return tokens.createAccessToken(
                match.getCreatedBy(),
                description,
                TimeUtil.getCurrentUTC().plus(config.getTokenExpiry()));
    }

    private Namespace requireOwnedNamespace(UserData user, String namespaceName) {
        Namespace namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }
        if (!repositories.isNamespaceOwner(user, namespace)) {
            throw new ErrorResultException("You must be an owner of this namespace.", HttpStatus.FORBIDDEN);
        }
        return namespace;
    }
}
