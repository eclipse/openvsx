/********************************************************************************
 * Copyright (c) 2023 Ericsson and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

/**
 * Encapsulate information about freshly authenticated users.
 *
 * Different OAuth2 providers may return the same information with different
 * attribute keys. This interface allows bridging arbitrary providers.
 */
public interface AuthUser {
    /**
     * @return Non-human readable unique identifier.
     */
    String getAuthId();
    /**
     * @return The user's avatar URL. Some services require post-processing to get the actual value for it
     * (the value returned is a template and you need to remplace variables).
     */
    String getAvatarUrl();
    /**
     * @return The user's email.
     */
    String getEmail();
    /**
     * @return The user's full name (first and last names).
     */
    String getFullName();
    /**
     * @return The login name for the user. Human-readable unique name. AKA username.
     */
    String getLoginName();
    /**
     * @return The authentication provider unique name, e.g. `github`, `eclipse`, etc.
     */
    String getProviderId();
    /**
     * @return The authentication provider URL.
     */
    String getProviderUrl();
}
