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

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * This class is not mapped to a database entity, but parsed / serialized to
 * JSON via a column converter.
 */
public class AuthToken {

    public String accessToken;

    public Instant issuedAt;

    public Instant expiresAt;

    public Set<String> scopes;
    
    public String refreshToken;

    public Instant refreshExpiresAt;

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof AuthToken))
            return false;
        var other = (AuthToken) obj;
        if (!Objects.equals(this.accessToken, other.accessToken))
            return false;
        if (!Objects.equals(this.issuedAt, other.issuedAt))
            return false;
        if (!Objects.equals(this.expiresAt, other.expiresAt))
            return false;
        if (!Objects.equals(this.scopes, other.scopes))
            return false;
        if (!Objects.equals(this.refreshToken, other.refreshToken))
            return false;
        if (!Objects.equals(this.refreshExpiresAt, other.refreshExpiresAt))
            return false;
        return true;
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