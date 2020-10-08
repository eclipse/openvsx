/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class IdPrincipal implements OidcUser, Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;

    public IdPrincipal(long id, String name) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }
    public long getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return new LinkedHashMap<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new HashSet<>();
    }

    @Override
    public Map<String, Object> getClaims() {
        return new LinkedHashMap<>();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return null;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Id: [");
		sb.append(this.getId());
		sb.append("], ");
		sb.append("Name: [");
		sb.append(this.getName());
		sb.append("]");
		return sb.toString();
	}
}