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

@Entity
public class FileResource {

    // Resource types
    public static final String DOWNLOAD = "download";
    public static final String DOWNLOAD_SHA256 = "sha256";
    public static final String DOWNLOAD_SIG = "signature";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String MANIFEST = "manifest";
    public static final String ICON = "icon";
    public static final String README = "readme";
    public static final String LICENSE = "license";
    public static final String CHANGELOG = "changelog";
    public static final String VSIXMANIFEST = "vsixmanifest";

    // Storage types
    public static final String STORAGE_LOCAL = "local";
    public static final String STORAGE_GOOGLE = "google-cloud";
    public static final String STORAGE_AZURE = "azure-blob";
    public static final String STORAGE_AWS = "aws";

    @Id
    @GeneratedValue(generator = "fileResourceSeq")
    @SequenceGenerator(name = "fileResourceSeq", sequenceName = "file_resource_seq")
    private long id;

    @OneToOne
    private ExtensionVersion extension;

    private String name;

    @Column(length = 32)
    private String type;

    @Basic(fetch = FetchType.LAZY)
    private byte[] content;

    @Column(length = 32)
    private String storageType;

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

    /**
     * @deprecated
     */
    @Deprecated
    public byte[] getContent() {
        return content;
    }

    public void clearContent() {
        this.content = null;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
}
