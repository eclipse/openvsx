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
package org.eclipse.openvsx.accesstoken;

import java.util.Objects;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;

/**
 * Access token scope.
 */
public sealed interface AccessTokenScope {
    /**
     * Checks for scope applicability, and returns {@code true} if action is applicable to this scope.
     */
    boolean allowsAction(AccessTokenAction accessTokenAction);

    /**
     * Unrestricted token scope; every action is applicable.
     */
    record Unrestricted() implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            return true;
        }
    }

    /**
     * Namespace scoped token scope; only actions providing {@link AccessTokenAction#namespace()} that matches
     * the namespace of this scope are applicable.
     */
    record NamespaceScoped(Namespace namespace) implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            return accessTokenAction.namespace().isPresent()
                    && Objects.equals(namespace.getName(), accessTokenAction.namespace().get());
        }
    }

    /**
     * Extension scoped token scope; only actions providing {@link AccessTokenAction#namespace()} and
     * {@link AccessTokenAction#extension()} that match the namespace and extension of this scope are applicable.
     */
    record ExtensionScoped(Extension extension) implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            return accessTokenAction.namespace().isPresent() && accessTokenAction.extension().isPresent() &&
                    Objects.equals(extension.getNamespace().getName(), accessTokenAction.namespace().get()) &&
                    Objects.equals(extension.getName(), accessTokenAction.extension().get());
        }
    }
}
