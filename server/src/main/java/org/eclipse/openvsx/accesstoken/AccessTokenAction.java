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

public sealed interface AccessTokenAction {
    /**
     * Action that verifies token only (does not "use" it).
     */
    record Verify() implements AccessTokenAction {}

    /**
     * Action that uses token for publishing.
     */
    record PublishVersion(String namespace, String extensionName) implements AccessTokenAction {}

    /**
     * Action that uses token for namespace creation.
     */
    record CreateNamespace(String namespace) implements AccessTokenAction {}

    /**
     * Action that uses token for administration.
     */
    record Administration() implements AccessTokenAction {}
}
