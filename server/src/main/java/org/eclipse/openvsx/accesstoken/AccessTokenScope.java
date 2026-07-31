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

/**
 * Access token scope.
 */
public sealed interface AccessTokenScope {
    /**
     * Checks for scope applicability.
     */
    boolean allowsAction(AccessTokenAction accessTokenAction);

    /**
     * Unrestricted token scope.
     */
    record Unrestricted() implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            return true;
        }
    }

    /**
     * Namespace scoped token scope.
     */
    record NamespaceScoped(String namespaceName) implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            if (accessTokenAction instanceof AccessTokenAction.CreateNamespace(String namespace)) {
                return Objects.equals(namespace, namespaceName);
            }
            return false;
        }
    }

    /**
     * Extension scoped token scope.
     */
    record ExtensionScoped(String namespaceName, String extensionName) implements AccessTokenScope {
        @Override
        public boolean allowsAction(AccessTokenAction accessTokenAction) {
            if (accessTokenAction instanceof AccessTokenAction.PublishVersion(String namespace, String extension)) {
                return Objects.equals(namespace, namespaceName) && Objects.equals(extension, extensionName);
            }
            return false;
        }
    }
}
