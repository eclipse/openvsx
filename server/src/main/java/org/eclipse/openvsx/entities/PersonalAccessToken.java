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

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.util.TimeUtil;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "value") })
public class PersonalAccessToken {

    @Id
    @GeneratedValue
    long id;

    @ManyToOne
    @JoinColumn(name = "user_data")
    UserData user;

    @Column(length = 64)
    String value;

    boolean active;

    LocalDateTime createdTimestamp;

    LocalDateTime accessedTimestamp;

    @Column(length = 2048)
    String description;

    @OneToMany(mappedBy = "publishedWith")
    List<ExtensionVersion> publishedVersions;


    /**
     * Convert to a JSON object.
     */
    public AccessTokenJson toAccessTokenJson() {
        var json = new AccessTokenJson();
        json.id = this.getId();
        // The value is not included: it is displayed only when the token is created
        if (this.getCreatedTimestamp() != null)
            json.createdTimestamp = TimeUtil.toUTCString(this.getCreatedTimestamp());
        if (this.getAccessedTimestamp() != null)
            json.accessedTimestamp = TimeUtil.toUTCString(this.getAccessedTimestamp());
        json.description = this.getDescription();
        return json;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
		this.id = id;
	}

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime timestamp) {
        this.createdTimestamp = timestamp;
    }

    public LocalDateTime getAccessedTimestamp() {
        return accessedTimestamp;
    }

    public void setAccessedTimestamp(LocalDateTime timestamp) {
        this.accessedTimestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}