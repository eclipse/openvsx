/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * This class is not mapped to a database entity, but parsed / serialized to
 * JSON via a column converter.
 */
public class AuthToken implements Serializable {

    public String accessToken;

    public Instant issuedAt;

    public Instant expiresAt;

    public Set<String> scopes;
    
    public String refreshToken;

    public Instant refreshExpiresAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthToken authToken = (AuthToken) o;
        return Objects.equals(accessToken, authToken.accessToken)
                && Objects.equals(issuedAt, authToken.issuedAt)
                && Objects.equals(expiresAt, authToken.expiresAt)
                && Objects.equals(scopes, authToken.scopes)
                && Objects.equals(refreshToken, authToken.refreshToken)
                && Objects.equals(refreshExpiresAt, authToken.refreshExpiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, issuedAt, expiresAt, scopes, refreshToken, refreshExpiresAt);
    }

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("accessToken: [");
		sb.append(accessToken);
		sb.append("], ");
		sb.append("refreshToken: [");
		sb.append(refreshToken);
		sb.append("], ");
		sb.append("expiresAt: [");
		sb.append(expiresAt);
		sb.append("], ");
		sb.append("scopes: [");
		sb.append(scopes);
        sb.append("], ");
        sb.append("refreshExpiresAt: [");
        sb.append(refreshExpiresAt);
        sb.append("]");
		return sb.toString();
    }

}