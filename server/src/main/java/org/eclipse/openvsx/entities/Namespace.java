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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = { "publicId" }),
		@UniqueConstraint(columnNames = { "name" })
})
public class Namespace implements Serializable {

    @Id
    @GeneratedValue
    long id;

    @Column(length = 128)
    String publicId;

    String name;

    @OneToMany(mappedBy = "namespace")
    List<Extension> extensions;

    @OneToMany(mappedBy = "namespace")
    List<NamespaceMembership> memberships;


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Namespace namespace = (Namespace) o;
		return id == namespace.id
				&& Objects.equals(publicId, namespace.publicId)
				&& Objects.equals(name, namespace.name)
				&& Objects.equals(extensions, namespace.extensions)
				&& Objects.equals(memberships, namespace.memberships);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, publicId, name, extensions, memberships);
	}
}