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

import jakarta.persistence.*;

import org.eclipse.openvsx.json.NamespaceMembershipJson;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
public class NamespaceMembership implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_CONTRIBUTOR = "contributor";

    @Id
    @GeneratedValue(generator = "namespaceMembershipSeq")
    @SequenceGenerator(name = "namespaceMembershipSeq", sequenceName = "namespace_membership_seq")
    private long id;

    @ManyToOne
    @JoinColumn(name = "namespace")
    private Namespace namespace;

    @ManyToOne
    @JoinColumn(name = "user_data")
    private UserData user;

    @Column(length = 32)
    private String role;

    public NamespaceMembershipJson toJson() {
        return new NamespaceMembershipJson(
                this.namespace.getName(),
                this.role,
                this.user.toUserJson()
        );
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

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceMembership that = (NamespaceMembership) o;
        return id == that.id
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(user, that.user)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, namespace, user, role);
    }
}