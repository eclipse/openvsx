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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Trust request; issued by owner of namespace, based on manual user input.
 */
public final class TrustRequest {
    @NonNull
    private final String namespaceName;
    @Nullable
    private final String extensionName;
    @NonNull
    private final String providerId;
    @NonNull
    private final String owner;
    @NonNull
    private final String repo;
    @NonNull
    private final String workflow;
    @Nullable
    private final String environment;

    public TrustRequest(@NonNull String namespaceName,
                        @Nullable String extensionName,
                        @NonNull String providerId,
                        @NonNull String owner,
                        @NonNull String repo,
                        @NonNull String workflow,
                        @Nullable String environment) {
        this.namespaceName = requireNonNull(namespaceName);
        this.extensionName = extensionName;
        this.providerId = requireNonNull(providerId);
        this.owner = requireNonNull(owner);
        this.repo = requireNonNull(repo);
        this.workflow = requireNonNull(workflow);
        this.environment = environment;
    }

    /**
     * The Open VSX namespace this trust is requested to.
     */
    @NonNull
    public String getNamespaceName() {
        return namespaceName;
    }

    /**
     * Optionally, the Open VSX extension name within {@link #getNamespaceName()} this trust is requested to.
     */
    @NonNull
    public Optional<String> getExtensionName() {
        return Optional.ofNullable(extensionName);
    }

    /**
     * The provider ID for the trust request.
     */
    @NonNull
    public String getProviderId() {
        return providerId;
    }

    /**
     * The owner of repository from where publishing is to happen from.
     */
    @NonNull
    public String getOwner() {
        return owner;
    }

    /**
     * The repository from where publishing is to happen from.
     */
    @NonNull
    public String getRepo() {
        return repo;
    }

    /**
     * The CI workflow filename, that publishing is to happen from.
     */
    @NonNull
    public String getWorkflow() {
        return workflow;
    }

    /**
     * Optionally, the environment CI workflow is deploying to.
     */
    @NonNull
    public Optional<String> getEnvironment() {
        return Optional.ofNullable(environment);
    }
}
