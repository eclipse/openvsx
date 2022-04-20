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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class FileResource {

    // Resource types
    public static final String DOWNLOAD = "download";
    public static final String MANIFEST = "manifest";
    public static final String ICON = "icon";
    public static final String README = "readme";
    public static final String LICENSE = "license";
    public static final String CHANGELOG = "changelog";
    public static final String RESOURCE = "resource";

    // Storage types
    public static final String STORAGE_DB = "database";
    public static final String STORAGE_GOOGLE = "google-cloud";
    public static final String STORAGE_AZURE = "azure-blob";

    @Id
    @GeneratedValue
    long id;

    @OneToOne
    ExtensionVersion extension;

    String name;

    @Column(length = 32)
    String type;

    byte[] content;

    @Column(length = 32)
    String storageType;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
}