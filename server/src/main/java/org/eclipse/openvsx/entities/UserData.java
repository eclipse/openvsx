/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.*;

import org.eclipse.openvsx.json.UserJson;

@Entity
public class UserData implements Serializable {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_PRIVILEGED = "privileged";

    @Id
    @GeneratedValue(generator = "userDataSeq")
    @SequenceGenerator(name = "userDataSeq", sequenceName = "user_data_seq")
    long id;

    @Column(length = 32)
    String role;

    String loginName;

    String fullName;

    String email;

    String avatarUrl;

    @Column(length = 32)
    String provider;

    String authId;

    String providerUrl;

    @OneToMany(mappedBy = "user")
    List<PersonalAccessToken> tokens;

    @OneToMany(mappedBy = "user")
    List<NamespaceMembership> memberships;

    @Column(length = 4096)
    @Convert(converter = EclipseDataConverter.class)
    EclipseData eclipseData;

    @Column(length = 4096)
    @Convert(converter = AuthTokenConverter.class)
    AuthToken githubToken;

    @Column(length = 4096)
    @Convert(converter = AuthTokenConverter.class)
    AuthToken eclipseToken;


    /**
     * Convert to a JSON object.
     */
    public UserJson toUserJson() {
        var json = new UserJson();
        json.loginName = this.getLoginName();
        json.fullName = this.getFullName();
        json.avatarUrl = this.getAvatarUrl();
        json.homepage = this.getProviderUrl();
        json.provider = this.getProvider();
        return json;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
		this.id = id;
	}

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public EclipseData getEclipseData() {
        return eclipseData;
    }

    public void setEclipseData(EclipseData eclipseData) {
        this.eclipseData = eclipseData;
    }

    public AuthToken getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(AuthToken githubToken) {
        this.githubToken = githubToken;
    }

    public AuthToken getEclipseToken() {
        return eclipseToken;
    }

    public void setEclipseToken(AuthToken eclipseToken) {
        this.eclipseToken = eclipseToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserData userData = (UserData) o;
        return id == userData.id
                && Objects.equals(role, userData.role)
                && Objects.equals(loginName, userData.loginName)
                && Objects.equals(fullName, userData.fullName)
                && Objects.equals(email, userData.email)
                && Objects.equals(avatarUrl, userData.avatarUrl)
                && Objects.equals(provider, userData.provider)
                && Objects.equals(authId, userData.authId)
                && Objects.equals(providerUrl, userData.providerUrl)
                && Objects.equals(tokens, userData.tokens)
                && Objects.equals(memberships, userData.memberships)
                && Objects.equals(eclipseData, userData.eclipseData)
                && Objects.equals(githubToken, userData.githubToken)
                && Objects.equals(eclipseToken, userData.eclipseToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, role, loginName, fullName, email, avatarUrl, provider, authId, providerUrl, tokens, memberships,
                eclipseData, githubToken, eclipseToken
        );
    }
}