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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class ExtensionBinary implements FileResource {

    @Id
    @GeneratedValue
    long id;

    @OneToOne
    ExtensionVersion extension;

    byte[] content;


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ExtensionVersion getExtension() {
		return extension;
	}

	public void setExtension(ExtensionVersion extension) {
		this.extension = extension;
	}

    @Override
	public byte[] getContent() {
		return content;
	}

    @Override
	public void setContent(byte[] content) {
		this.content = content;
	}

}