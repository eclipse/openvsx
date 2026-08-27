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
package org.eclipse.openvsx.util.auth;

import org.jspecify.annotations.NonNull;

import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.UserData;

/**
 * Represents user who presented a valid access token.
 */
public record AccessTokenAuthentication(UserData userData, PersonalAccessTokenType type) implements AuthenticatedUser {
    @Override
    public @NonNull AuthenticationType authenticationType() {
        return AuthenticationType.TOKEN;
    }
}
