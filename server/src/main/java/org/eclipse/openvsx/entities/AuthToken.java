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
		sb.append("expiresAt: [");
		sb.append(expiresAt);
		sb.append("], ");
		sb.append("scopes: [");
		sb.append(scopes);
		sb.append("]");
		return sb.toString();
    }

}